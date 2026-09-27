package com.bioinformatics.analyticsservice.materializeviews.service;

import com.bioinformatics.analyticsservice.config.ApplicationProperties;
import com.bioinformatics.analyticsservice.materializeviews.dto.RefreshResult;
import com.bioinformatics.analyticsservice.materializeviews.dto.ViewToRefresh;
import com.bioinformatics.analyticsservice.materializeviews.entity.ViewRefreshLog;
import com.bioinformatics.analyticsservice.materializeviews.repository.ViewRefreshLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Service for refreshing materialized views asynchronously.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Execute REFRESH MATERIALIZED VIEW commands for each analytics view
 *   <li>Implement retry logic with exponential backoff
 *   <li>Log audit trail for each refresh attempt
 *   <li>Alert on SLA breaches (>sequenceSlaMs) or view failures
 * </ul>
 *
 * <p>Views to Refresh (in order):
 * <ol>
 *   <li>mv_dashboard_kpis (non-concurrent, 100ms typical)
 *   <li>mv_length_histogram (concurrent, 200ms typical)
 *   <li>mv_organism_counts (concurrent, 300ms typical)
 *   <li>mv_reviewed_ratio (concurrent, 150ms typical)
 *   <li>mv_evidence_distribution (concurrent, 200ms typical)
 *   <li>mv_keyword_frequency (concurrent, 400ms typical)
 * </ol>
 *
 * <p>Called by import/export services after data ingestion.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MaterializedViewRefreshService {

    private final JdbcTemplate jdbcTemplate;
    private final ViewRefreshLogRepository logRepository;
    private final ApplicationProperties appProperties;
    private static final List<ViewToRefresh> viewsToRefreshPlan = List.of(
            new ViewToRefresh("mv_dashboard_kpis", false),
            new ViewToRefresh("mv_length_histogram", true),
            new ViewToRefresh("mv_organism_counts", true),
            new ViewToRefresh("mv_reviewed_ratio", true),
            new ViewToRefresh("mv_evidence_distribution", true),
            new ViewToRefresh("mv_keyword_frequency", true)
    );
    private final ViewRefreshAlertService alertService;

    /**
     * Refreshes all materialized views asynchronously.
     *
     * <p>Workflow:
     * <ol>
     *   <li>Execute refresh for each view in sequence
     *   <li>Log results to ViewRefreshLog table
     *   <li>Calculate total sequence duration
     *   <li>Alert on failures or SLA breaches
     * </ol>
     *
     * @param jobId import/export job identifier for audit trail
     */
    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void refreshAllDashboardViews(String jobId) {
        log.info("[VIEW_REFRESH] Starting materialized views refresh sequence - jobId={}", jobId);
        var startTime = System.currentTimeMillis();

        var failedViews = viewsToRefreshPlan
                .stream()
                .map(e -> {
                    log.debug("[VIEW_REFRESH] Executing refresh - jobId={}, view={}", jobId, e.viewName());
                    return executeAndLogRefresh(jobId, e);
                })
                .filter(e -> !e.success())
                .toList();

        var duration = System.currentTimeMillis() - startTime;
        var sequenceSlaMs = appProperties.viewRefresh().sequenceSlaMs();

        if (!failedViews.isEmpty()) {
            var views = failedViews.stream().map(RefreshResult::viewName).toList();
            log.error("[VIEW_REFRESH] View refresh failures detected - jobId={}, failedViews={}, duration={}ms",
                    jobId, views, duration);
            alertService.alertRefreshSequenceFailure(jobId, views, duration);
        }

        if (duration > sequenceSlaMs) {
            log.warn("[VIEW_REFRESH] View refresh SLA breach - jobId={}, duration={}ms, sla={}ms",
                    jobId, duration, sequenceSlaMs);
            alertService.alertRefreshSequenceSlaBreach(jobId, duration, sequenceSlaMs);
        }

        log.info("[VIEW_REFRESH] Refresh all views completed - jobId={}, duration={}ms, failedCount={}, slaCompliance={}",
                jobId, duration, failedViews.size(), duration <= sequenceSlaMs ? "YES" : "NO");
    }

    /**
     * Executes and logs a single view refresh with retry logic.
     *
     * <p>Strategy:
     * <ul>
     *   <li>Attempt refresh up to maxAttempts times
     *   <li>Use exponential backoff between retries
     *   <li>Apply per-view timeout to prevent hanging
     *   <li>Log result (success/failure) to database immediately
     * </ul>
     *
     * <p>REQUIRES_NEW transaction ensures each log is durable regardless
     * of outer transaction success/failure.
     *
     * @param jobId job identifier for audit trail
     * @param view view definition (name, concurrent mode)
     * @return refresh result (success/failure)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RefreshResult executeAndLogRefresh(String jobId, ViewToRefresh view) {
        var refreshProps = appProperties.viewRefresh();
        var maxAttempts = refreshProps.maxAttempts();
        var timeoutMs = refreshProps.perViewTimeoutMs();
        var retryBackoffMs = refreshProps.retryBackoffMs();

        log.debug("[VIEW_REFRESH] Preparing refresh - view={}, maxAttempts={}, timeoutMs={}",
                view.viewName(), maxAttempts, timeoutMs);

        var auditLog = ViewRefreshLog.builder()
                .jobIdentifier(jobId)
                .viewName(view.viewName())
                .executedAt(LocalDateTime.now())
                .build();

        var query = "REFRESH MATERIALIZED VIEW "
                .concat(view.concurrently() ? "CONCURRENTLY " : "")
                .concat(sanitizeIdentifier(view.viewName()));

        log.debug("[VIEW_REFRESH] Executing refresh query - view={}, concurrent={}, query={}",
                view.viewName(), view.concurrently(), query);

        for (var attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                executeRefreshWithTimeout(query, timeoutMs);
                log.info("[VIEW_REFRESH] Refresh succeeded - view={}, attempt={}/{}",
                        view.viewName(), attempt, maxAttempts);
                auditLog.setSuccess(true);
                auditLog.setErrorMessage(null);
                logRepository.save(auditLog);
                return new RefreshResult(view.viewName(), true);
            } catch (Exception e) {
                log.warn("[VIEW_REFRESH] Refresh failed - view={}, attempt={}/{}, reason='{}'",
                        view.viewName(), attempt, maxAttempts, e.getMessage());
                if (attempt == maxAttempts) {
                    var errorMessage = Objects.requireNonNullElse(e.getMessage(), e.getClass().getName());
                    log.error("[VIEW_REFRESH] View refresh exhausted all attempts - view={}, finalError='{}'",
                            view.viewName(), errorMessage);
                    auditLog.setSuccess(false);
                    auditLog.setErrorMessage(errorMessage);
                    logRepository.save(auditLog);
                    alertService.alertViewRefreshFailure(jobId, view.viewName(), maxAttempts, timeoutMs, errorMessage);
                    return new RefreshResult(view.viewName(), false);
                }
                pauseBeforeRetry(retryBackoffMs, attempt, view.viewName());
            }
        }
        return new RefreshResult(view.viewName(), false);
    }


    /**
     * Pauses execution with exponential backoff before retry attempt.
     *
     * @param retryBackoffMs base backoff milliseconds
     * @param attempt        current attempt number (multiplies backoff)
     * @param viewName       view being refreshed (for logging)
     */
    private void pauseBeforeRetry(long retryBackoffMs, int attempt, String viewName) {
        var backoff = retryBackoffMs * attempt;
        if (backoff <= 0) {
            return;
        }
        try {
            log.debug("[VIEW_REFRESH] Retry backoff - view={}, attempt={}, backoffMs={}",
                    viewName, attempt, backoff);
            TimeUnit.MILLISECONDS.sleep(backoff);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            log.warn("[VIEW_REFRESH] Retry backoff interrupted - view={}, attempt={}",
                    viewName, attempt, interruptedException);
        }
    }

    /**
     * Executes SQL refresh with statement timeout.
     *
     * <p>Sets PostgreSQL statement_timeout to prevent infinite hangs.
     * Restores default timeout after execution.
     *
     * @param refreshQuery REFRESH MATERIALIZED VIEW SQL command
     * @param timeoutMs timeout in milliseconds
     */
    private void executeRefreshWithTimeout(String refreshQuery, long timeoutMs) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (var statement = connection.createStatement()) {
                statement.execute("SET statement_timeout = " + timeoutMs);
                statement.execute("SET search_path TO analytics, public");
                log.debug("[VIEW_REFRESH] Executing view refresh - query={}, timeoutMs={}", refreshQuery, timeoutMs);
                statement.execute(refreshQuery);
            } finally {
                try (var resetStatement = connection.createStatement()) {
                    resetStatement.execute("SET statement_timeout = DEFAULT");
                    resetStatement.execute("RESET search_path");
                }
            }
            return null;
        });
    }

    /**
     * Sanitizes SQL identifier to prevent injection.
     *
     * @param identifier table/view name
     * @return quoted, escaped identifier
     * @throws IllegalArgumentException if identifier is null/empty
     */
    private String sanitizeIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }
        String escaped = identifier.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
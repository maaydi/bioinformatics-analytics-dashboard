package com.bioinformatics.dashboard.batch.service;

import com.bioinformatics.dashboard.batch.model.RefreshResult;
import com.bioinformatics.dashboard.batch.model.ViewToRefresh;
import com.bioinformatics.dashboard.config.AppProperties;
import com.bioinformatics.dashboard.job.entity.ViewRefreshLog;
import com.bioinformatics.dashboard.job.repository.ViewRefreshLogRepository;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class MaterializedViewRefreshService {

    private final JdbcTemplate jdbcTemplate;
    private final ViewRefreshLogRepository logRepository;
    private static final List<ViewToRefresh> viewsToRefreshPlan = List.of(
            new ViewToRefresh("mv_dashboard_kpis", false),
            new ViewToRefresh("mv_length_histogram", true),
            new ViewToRefresh("mv_organism_counts", true),
            new ViewToRefresh("mv_reviewed_ratio", true),
            new ViewToRefresh("mv_evidence_distribution", true),
            new ViewToRefresh("mv_keyword_frequency", true)
    );
    private final ViewRefreshAlertService alertService;
    private final AppProperties appProperties;

    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void refreshAllDashboardViews(String jobId) {
        log.info("Starting materialized views refresh sequence for job: {}", jobId);
        var startTime = System.currentTimeMillis();

        var failedViews = viewsToRefreshPlan
                .stream()
                .map(e -> executeAndLogRefresh(jobId, e))
                .filter(e -> !e.success())
                .toList();
        var duration = System.currentTimeMillis() - startTime;
        var sequenceSlaMs = appProperties.getViewRefresh().getSequenceSlaMs();

        if (!failedViews.isEmpty()) {
            var views = failedViews.stream().map(RefreshResult::viewName).toList();
            alertService.alertRefreshSequenceFailure(jobId, views, duration);
        }
        if (duration > sequenceSlaMs) {
            alertService.alertRefreshSequenceSlaBreach(jobId, duration, sequenceSlaMs);
        }
        log.info("Refresh all views completed in {} ms.", duration);
    }

    /**
     * REQUIRES_NEW ensures that each view log is written to the database immediately
     * in its own transaction. A failure in Postgres won't roll back the log entry.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RefreshResult executeAndLogRefresh(String jobId, ViewToRefresh view) {
        var refreshProps = appProperties.getViewRefresh();
        var maxAttempts = refreshProps.getMaxAttempts();
        var timeoutMs = refreshProps.getPerViewTimeoutMs();
        var retryBackoffMs = refreshProps.getRetryBackoffMs();

        var auditLog = ViewRefreshLog.builder()
                .jobIdentifier(jobId)
                .viewName(view.viewName())
                .executedAt(LocalDateTime.now())
                .build();

        var query = "REFRESH MATERIALIZED VIEW "
                .concat(view.concurrently() ? "CONCURRENTLY " : "")
                .concat(sanitizeIdentifier(view.viewName()));

        for (var attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                executeRefreshWithTimeout(query, timeoutMs);
                log.info("Refresh '{}' completed successfully on attempt {}/{}", view.viewName(), attempt, maxAttempts);
                auditLog.setSuccess(true);
                auditLog.setErrorMessage(null);
                logRepository.save(auditLog);
                return new RefreshResult(view.viewName(), true);
            } catch (Exception e) {
                log.warn("Refresh '{}' failed on attempt {}/{}", view.viewName(), attempt, maxAttempts, e);
                if (attempt == maxAttempts) {
                    var errorMessage = Objects.requireNonNullElse(e.getMessage(), e.getClass().getName());
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


    private void pauseBeforeRetry(long retryBackoffMs, int attempt, String viewName) {
        var backoff = retryBackoffMs * attempt;
        if (backoff <= 0) {
            return;
        }
        try {
            TimeUnit.MILLISECONDS.sleep(backoff);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            log.warn("Retry backoff interrupted for view '{}'", viewName, interruptedException);
        }
    }

    private void executeRefreshWithTimeout(String refreshQuery, long timeoutMs) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (var statement = connection.createStatement()) {
                statement.execute("SET statement_timeout = " + timeoutMs);
                statement.execute(refreshQuery);
            } finally {
                try (var resetStatement = connection.createStatement()) {
                    resetStatement.execute("SET statement_timeout = DEFAULT");
                }
            }
            return null;
        });
    }

    private String sanitizeIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }
        String escaped = identifier.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
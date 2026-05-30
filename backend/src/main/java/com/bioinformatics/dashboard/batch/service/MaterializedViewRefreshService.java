package com.bioinformatics.dashboard.batch.service;

import com.bioinformatics.dashboard.job.entity.ViewRefreshLog;
import com.bioinformatics.dashboard.job.repository.ViewRefreshLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class MaterializedViewRefreshService {

    private final JdbcTemplate jdbcTemplate;
    private final ViewRefreshLogRepository logRepository;

    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void refreshAllDashboardViews(String jobId) {
        log.info("Starting materialized views refresh sequence for job: {}", jobId);
        long startTime = System.currentTimeMillis();

        var viewsToRefresh = LinkedHashMap.newLinkedHashMap(6);
        viewsToRefresh.put("mv_dashboard_kpis", false);
        viewsToRefresh.put("mv_length_histogram", true);
        viewsToRefresh.put("mv_organism_counts", true);
        viewsToRefresh.put("mv_reviewed_ratio", true);
        viewsToRefresh.put("mv_evidence_distribution", true);
        viewsToRefresh.put("mv_keyword_frequency", true);
        for (var entry : viewsToRefresh.entrySet()) {
            executeAndLogRefresh(jobId, (String) entry.getKey(), (boolean) entry.getValue());
        }
        long duration = System.currentTimeMillis() - startTime;
        log.info("Refresh all views completed in {} ms.", duration);
    }

    /**
     * REQUIRES_NEW ensures that each view log is written to the database immediately
     * in its own transaction. A failure in Postgres won't roll back the log entry.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeAndLogRefresh(String jobId, String viewName, boolean concurrently) {
        var auditLog = ViewRefreshLog.builder()
                .jobIdentifier(jobId)
                .viewName(viewName)
                .executedAt(LocalDateTime.now())
                .build();
        var query = "REFRESH MATERIALIZED VIEW "
                .concat(concurrently ? "CONCURRENTLY " : "")
                .concat(sanitizeIdentifier(viewName));
        try {
            jdbcTemplate.execute(query);
            log.info("Refresh '{}' completed successfully", viewName);
            auditLog.setSuccess(true);
        } catch (Exception e) {
            log.error("Critical error while refreshing materialized view '{}'", viewName, e);
            auditLog.setSuccess(false);
            auditLog.setErrorMessage(e.getMessage() != null ? e.getMessage() : e.getClass().getName());
        } finally {
            logRepository.save(auditLog);
        }
    }

    private String sanitizeIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }
        String escaped = identifier.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
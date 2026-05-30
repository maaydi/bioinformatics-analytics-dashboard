package com.bioinformatics.dashboard.analytics.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MaterializedViewRefreshService {

    private final JdbcTemplate jdbcTemplate;

    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void refreshAllDashboardViews() {
        log.info("Starting the materialized views refresh sequence...");
        long startTime = System.currentTimeMillis();
        refreshView("mv_dashboard_kpis", false);
        refreshView("mv_length_histogram", true);
        refreshView("mv_organism_counts", true);
        refreshView("mv_reviewed_ratio", true);
        refreshView("mv_evidence_distribution", true);
        refreshView("mv_keyword_frequency", true);
        long duration = System.currentTimeMillis() - startTime;
        log.info("Refresh all views completed in {} ms.", duration);
    }

    private void refreshView(String viewName, boolean concurrently) {
        var query = "REFRESH MATERIALIZED VIEW "
                .concat(concurrently ? "CONCURRENTLY " : "")
                .concat(viewName);
        try {
            jdbcTemplate.execute(query);
            log.info("Refresh '{}' completed successfully", viewName);
        } catch (Exception e) {
            log.error("Critical error while refreshing materialized view '{}'", viewName, e);
        }

    }
}

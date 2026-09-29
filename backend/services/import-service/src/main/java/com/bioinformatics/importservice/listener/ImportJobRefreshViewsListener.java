package com.bioinformatics.importservice.listener;

import com.bioinformatics.importservice.dto.Constants;
import com.bioinformatics.importservice.service.MaterializedViewRefreshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Job lifecycle listener that refreshes materialized views after successful import.
 *
 * <p>Called after job completion to rebuild analytical views and denormalized data
 * based on newly imported protein data.
 *
 * <p>Only executes on COMPLETED status; skips on FAILED.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImportJobRefreshViewsListener implements JobExecutionListener {

    private final MaterializedViewRefreshService refreshService;

    /**
     * Called after import job completes successfully.
     *
     * <p>Triggers refresh of materialized views to incorporate new data.
     * Extracted source information (file path or filter name) for audit logging.
     *
     * @param jobExecution batch job execution
     */
    @Override
    public void afterJob(JobExecution jobExecution) {
        var jobId = jobExecution.getJobParameters().getString(Constants.IMPORT_JOB_ID.getKey());
        if (jobId == null) {
            log.debug("[VIEWS_LISTENER] No job ID in parameters - skipping view refresh");
            return;
        }

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            var file = jobExecution.getJobParameters().getString(Constants.FILE_PATH.getKey());
            var filter = jobExecution.getJobParameters().getLong(Constants.SAVED_FILTER_ID.getKey());

            if (file != null || filter != null) {
                var source = file != null
                        ? "File ".concat(file)
                        : "Remote API for Saved filter ID ".concat(String.valueOf(filter));
                log.info("[VIEWS_LISTENER] Refreshing materialized views after import - ID={}, source='{}'",
                        jobId, source);
                refreshService.refreshAllDashboardViews(jobId, source);
                log.info("[VIEWS_LISTENER] Materialized views refresh complete - ID={}", jobId);
            } else {
                log.warn("[VIEWS_LISTENER] Could not identify import source - ID={}, skipping view refresh", jobId);
            }
        } else {
            log.debug("[VIEWS_LISTENER] Job did not complete successfully - status={}, skipping view refresh",
                    jobExecution.getStatus());
        }
    }
}
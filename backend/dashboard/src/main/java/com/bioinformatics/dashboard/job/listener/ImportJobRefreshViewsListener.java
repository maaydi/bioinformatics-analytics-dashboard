package com.bioinformatics.dashboard.job.listener;

import com.bioinformatics.dashboard.job.dto.Constants;
import com.bioinformatics.dashboard.job.service.MaterializedViewRefreshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImportJobRefreshViewsListener implements JobExecutionListener {

    /**
     * Persists import job results to the database after job completion.
     * Updates status, processed counts, duration and error message when applicable.
     */

    private final MaterializedViewRefreshService refreshService;

    @Override
    public void afterJob(JobExecution jobExecution) {
        var jobId = jobExecution.getJobParameters().getString(Constants.IMPORT_JOB_ID.getKey());
        if (jobId == null)
            return;
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            var file = jobExecution.getJobParameters().getString(Constants.FILE_PATH.getKey());
            log.info("Refresh Materialized views after execution job <{}> on file <{}>", jobId, file);
            refreshService.refreshAllDashboardViews(jobId);
        }
    }
}
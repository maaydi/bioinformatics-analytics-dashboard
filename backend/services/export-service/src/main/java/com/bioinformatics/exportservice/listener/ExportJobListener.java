package com.bioinformatics.exportservice.listener;

import com.bioinformatics.exportservice.service.ExportPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

import static com.bioinformatics.exportservice.dto.Constants.EXPORT_JOB_ID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExportJobListener implements JobExecutionListener {

    private final ExportPipelineService exportPipelineService;

    @Override
    public void afterJob(@NonNull JobExecution jobExecution) {
        var parameters = jobExecution.getJobParameters();
        var pipelineId = parameters.getLong(EXPORT_JOB_ID.getKey());
        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            exportPipelineService.markAsFailed(pipelineId);
        }
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        var jobParams = jobExecution.getJobParameters();
        var pipelineId = jobParams.getLong(EXPORT_JOB_ID.getKey());
        exportPipelineService.startPipeline(pipelineId);
    }
}

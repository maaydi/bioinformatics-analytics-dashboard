package com.bioinformatics.exportservice.listener;

import com.bioinformatics.exportservice.service.ExportPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.bioinformatics.exportservice.dto.Constants.EXPORT_JOB_ID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExportJobLifecycleListener implements JobExecutionListener {

    private final ExportPipelineService pipelineService;

    @Override
    public void beforeJob(@NonNull JobExecution jobExecution) {
        var pipelineId = getPipelineId(jobExecution);
        pipelineService.markAsRunning(pipelineId, jobExecution.getId());
    }

    @Override
    public void afterJob(@NonNull JobExecution jobExecution) {
        var pipelineId = getPipelineId(jobExecution);
        switch (jobExecution.getStatus()) {
            case FAILED:
                var errorMessage = extractFailureMessage(jobExecution);
                pipelineService.markAsFailed(pipelineId, errorMessage);
                break;
            case COMPLETED:
                pipelineService.markAsCompleted(pipelineId, null, null, null);
                break;
            default:
                pipelineService.markAsFailed(jobExecution.getId(), "Export Job did not stop properly");
        }
    }

    private Long getPipelineId(JobExecution jobExecution) {
        return jobExecution
                .getJobParameters()
                .getLong(EXPORT_JOB_ID.getKey());
    }

    private String extractFailureMessage(JobExecution jobExecution) {
        return jobExecution.getAllFailureExceptions()
                .stream()
                .map(Throwable::getMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Export job failed");
    }
}
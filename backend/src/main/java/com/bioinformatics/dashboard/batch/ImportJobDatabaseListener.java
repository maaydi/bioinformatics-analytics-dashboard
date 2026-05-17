package com.bioinformatics.dashboard.batch;

import com.bioinformatics.dashboard.job.dto.Constants;
import com.bioinformatics.dashboard.job.dto.ImportStatus;
import com.bioinformatics.dashboard.job.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZoneId;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImportJobDatabaseListener implements JobExecutionListener {

    /**
     * Persists import job results to the database after job completion.
     * Updates status, processed counts, duration and error message when applicable.
     */

    private final ImportJobRepository importJobRep;

    @Override
    public void afterJob(JobExecution jobExecution) {
        var jobId = jobExecution.getJobParameters().getString(Constants.IMPORT_JOB_ID.getKey());
        if (jobId == null)
            return;

        var importJobId = UUID.fromString(jobId);
        var jobRecord = importJobRep.findById(importJobId).orElse(null);

        if (jobRecord == null)
            return;

        var totalProcessed = jobExecution.getStepExecutions()
                .stream()
                .mapToLong(StepExecution::getWriteCount)
                .sum();

        assert jobExecution.getEndTime() != null;
        var durationMs = Duration.between(
                jobExecution.getCreateTime().atZone(ZoneId.systemDefault()).toInstant(),
                jobExecution.getEndTime().atZone(ZoneId.systemDefault()).toInstant()).toMillis();

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            jobRecord.setStatus(ImportStatus.COMPLETED);
        } else {
            jobRecord.setStatus(ImportStatus.FAILED);
            if (!jobExecution.getAllFailureExceptions().isEmpty()) {
                jobRecord.setErrorMessage(jobExecution.getAllFailureExceptions().getFirst().getMessage());
            }
        }

        jobRecord.setRecordsProcessed((int) totalProcessed);
        jobRecord.setEntryCount((int) totalProcessed);
        jobRecord.setDurationMs(durationMs);
        jobRecord.setCompletedAt(jobExecution.getEndTime().atZone(ZoneId.systemDefault()).toInstant());

        importJobRep.save(jobRecord);
        log.info("uniprot Data for Job <{}> successfully imported", jobId);
    }
}
package com.bioinformatics.dashboard.job.service;

import com.bioinformatics.dashboard.job.dto.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static com.bioinformatics.dashboard.job.dto.Constants.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobRecoveryRunner implements ApplicationRunner {

    private final JobRepository jobRepository;
    private final JobOperator jobOperator;
    private final ImportJobRecoveryService importJobRecovery;


    @Override
    public void run(@NonNull ApplicationArguments args) throws Exception {
        Stream.of(IMPORT_FILE_JOB, IMPORT_API_JOB)
                .map(Constants::getKey)
                .map(jobRepository::getLastJobInstance)
                .filter(Objects::nonNull)
                .map(jobRepository::getJobExecutions)
                .flatMap(List::stream)
                .filter(jobExecution -> jobExecution.getStatus() != BatchStatus.COMPLETED
                        // assert import job is saved in ImportJob table
                        && Objects.nonNull(jobExecution.getJobParameters().getString(IMPORT_JOB_ID.getKey())))
                .forEach(job -> {
                    var jobId = UUID.fromString(Objects.requireNonNull(job.getJobParameters().getString(IMPORT_JOB_ID.getKey())));
                    var jobname = job.getJobInstance().getJobName();
                    log.info("Attempting to restart Job {} with Batch Execution ID {}", jobname, job.getId());
                    try {
                        jobOperator.restart(job);
                    } catch (Exception e) {
                        log.error("Failed to restart {} : {}", jobname, e.getMessage());
                        importJobRecovery.markImportJobAsFailed(jobId);
                    }
                });
    }
}


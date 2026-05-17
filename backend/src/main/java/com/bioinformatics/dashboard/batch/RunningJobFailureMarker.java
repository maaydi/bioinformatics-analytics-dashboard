package com.bioinformatics.dashboard.batch;

import com.bioinformatics.dashboard.job.dto.ImportStatus;
import com.bioinformatics.dashboard.job.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RunningJobFailureMarker implements ApplicationRunner {

    /**
     * On application start, marks any previously RUNNING import jobs as FAILED and
     * cleans up non-completed job executions to avoid dangling state after restarts.
     */

    private final ImportJobRepository importJobRepo;
    private final JobRepository jobRepo;

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) throws Exception {
        log.info("Clean previous incomplete jobs");
        jobRepo.getJobNames()
                .stream()
                .map(jobRepo::findJobInstances)
                .flatMap(List::stream)
                .map(JobInstance::getJobExecutions)
                .flatMap(List::stream)
                .filter(jobExecution -> jobExecution.getStatus() != BatchStatus.COMPLETED)
                .forEach(jobRepo::deleteJobExecution);
        importJobRepo.updateStatusInBulk(ImportStatus.RUNNING, ImportStatus.FAILED);
    }
}

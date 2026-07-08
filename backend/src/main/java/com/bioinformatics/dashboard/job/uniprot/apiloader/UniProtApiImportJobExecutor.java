package com.bioinformatics.dashboard.job.uniprot.apiloader;

import com.bioinformatics.dashboard.job.dto.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class UniProtApiImportJobExecutor implements CommandLineRunner {

    private final JobOperator operator;
    private final Job uniProtApiImportJob;
    private final JobRepository jobRepo;

    @Override
    @Async("importExecutor")
    public void run(String @NonNull ... args) {
        try {
            log.info("Starting UniProt API Import Job on application startup...");

            var lastInstance = jobRepo.getLastJobInstance(Constants.AUTOMATIC_API_IMPORT_JOB.getKey());
            if (lastInstance == null) {
                log.info("No previous jobs found for auto API Import job, starting new job...");
                var parameters = new JobParametersBuilder()
                        .addLong("timestamp", System.currentTimeMillis())
                        .toJobParameters();

                log.info("UniProt API import job started asynchronously");
                operator.start(uniProtApiImportJob, parameters);
            } else {
                log.info("Found previous job instance for auto API Import job.");
                var lastExec = jobRepo.getLastJobExecution(lastInstance);
                if (lastExec != null) {
                    switch (lastExec.getStatus()) {
                        case STARTED, STARTING, STOPPING, FAILED:
                            log.info("Last UniProt API Import job is still running, recovering and restarting...");
                            operator.recover(lastExec);
                            operator.restart(lastExec);
                            break;
                        default:
                            log.info("Last UniProt API Import job is not running, no action needed.");
                            break;
                    }
                } else {
                    log.info("Last UniProt API Import job completed successfully, no action needed.");
                }
            }
        } catch (Exception e) {
            log.error("Failed to execute UniProt API Import Job at startup", e);
        }
    }
}

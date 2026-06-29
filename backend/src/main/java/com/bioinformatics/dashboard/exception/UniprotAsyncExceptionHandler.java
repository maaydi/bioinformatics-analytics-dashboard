package com.bioinformatics.dashboard.exception;

import com.bioinformatics.dashboard.job.dto.ImportStatus;
import com.bioinformatics.dashboard.job.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

import static com.bioinformatics.dashboard.job.dto.Constants.IMPORT_JOB_ID;

/**
 * Manages operations and logic for UniprotAsyncExceptionHandler.
 */
@Component
@RequiredArgsConstructor
public class UniprotAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(UniprotAsyncExceptionHandler.class);
    private final ImportJobRepository importJobRep;

    @Override
    public void handleUncaughtException(@NonNull Throwable ex, Method method, Object... params) {
        logger.error("Unexpected error occurred executing async method: {}", method.getName(), ex);

        for (Object param : params) {
            if (param instanceof JobParameters jobParameters) {
                var jobIdStr = jobParameters.getString(IMPORT_JOB_ID.getKey());
                if (jobIdStr != null) {
                    updateJobStatusToFailed(jobIdStr, ex.getMessage());
                }
                break;
            }
        }
    }

    private void updateJobStatusToFailed(String jobIdStr, String errorMessage) {
        try {
            var jobId = UUID.fromString(jobIdStr);
            importJobRep.findById(jobId).ifPresent(job -> {
                job.setStatus(ImportStatus.FAILED);
                job.setErrorMessage("Failed to start job: " + errorMessage);
                importJobRep.save(job);
                logger.info("Successfully updated Job ID {} status to FAILED", jobId);
            });
        } catch (Exception dbEx) {
            logger.error("Critical failure: Could not update status for Job ID {} to FAILED", jobIdStr, dbEx);
        }
    }
}
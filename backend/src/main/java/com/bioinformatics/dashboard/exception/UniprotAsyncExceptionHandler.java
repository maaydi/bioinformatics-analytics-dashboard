package com.bioinformatics.dashboard.exception;

import java.lang.reflect.Method;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.stereotype.Component;

import com.bioinformatics.dashboard.job.dto.ImportStatus;
import com.bioinformatics.dashboard.job.repository.ImportJobRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UniprotAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(UniprotAsyncExceptionHandler.class);
    private final ImportJobRepository importJobRep;

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        logger.error("Unexpected error occurred executing async method: {}", method.getName(), ex);

        for (Object param : params) {
            if (param instanceof JobParameters) {
                JobParameters jobParameters = (JobParameters) param;
                String jobIdStr = jobParameters.getString("importUniprotJobId");

                if (jobIdStr != null) {
                    updateJobStatusToFailed(jobIdStr, ex.getMessage());
                }
                break;
            }
        }
    }

    private void updateJobStatusToFailed(String jobIdStr, String errorMessage) {
        try {
            UUID jobId = UUID.fromString(jobIdStr);
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
package com.bioinformatics.importservice.batch.listener;

import com.bioinformatics.importservice.dto.Constants;
import com.bioinformatics.importservice.listener.ImportJobRefreshViewsListener;
import com.bioinformatics.importservice.service.MaterializedViewRefreshService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ImportJobRefreshViewsListenerTest {

    private final String jobId = UUID.randomUUID().toString();
    @Mock
    private MaterializedViewRefreshService refreshService;
    private ImportJobRefreshViewsListener listener;
    private JobExecution jobExecution;

    @BeforeEach
    void setUp() {
        listener = new ImportJobRefreshViewsListener(refreshService);
        JobParameters params = new JobParametersBuilder()
                .addString(Constants.IMPORT_JOB_ID.getKey(), jobId)
                .addString(Constants.FILE_PATH.getKey(), "/tmp/test.dat")
                .toJobParameters();

        jobExecution = new JobExecution(1L, new org.springframework.batch.core.job.JobInstance(1L, "job"), params);
    }

    @Test
    void afterJob_whenCompleted_refreshesViews() {
        jobExecution.setStatus(BatchStatus.COMPLETED);

        listener.afterJob(jobExecution);

        verify(refreshService).refreshAllDashboardViews(jobId, "");
    }

    @Test
    void afterJob_whenFailed_doesNotRefreshViews() {
        jobExecution.setStatus(BatchStatus.FAILED);

        listener.afterJob(jobExecution);

        verifyNoInteractions(refreshService);
    }

    @Test
    void afterJob_withMissingJobId_returnsEarly() {
        jobExecution = org.springframework.batch.test.MetaDataInstanceFactory.createJobExecution("job", 1L, 1L, new JobParameters());
        jobExecution.setStatus(BatchStatus.COMPLETED);

        listener.afterJob(jobExecution);

        verifyNoInteractions(refreshService);
    }
}

package com.bioinformatics.dashboard.batch;

import com.bioinformatics.dashboard.exception.ExecuteJobException;
import com.bioinformatics.dashboard.job.uniprot.fileloader.AsyncUniprotImportJobExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncUniprotImportJobExecutorTest {

    @Mock
    private JobOperator jobOperator;

    @Mock
    private Job uniProtImportJob;

    @InjectMocks
    private AsyncUniprotImportJobExecutor executor;

    @Test
    void execute_successfulJobLaunch() throws Exception {
        // Arrange
        JobParameters parameters = new JobParametersBuilder()
                .addString("jobId", "123")
                .toJobParameters();

        JobExecution mockExecution = mock(JobExecution.class);
        when(jobOperator.start(any(Job.class), any(JobParameters.class))).thenReturn(mockExecution);

        // Act
        executor.execute(parameters);

        // Assert
        verify(jobOperator).start(uniProtImportJob, parameters);
    }

    @Test
    void execute_exceptionOnJobLaunch_throwsExecuteJobException() throws Exception {
        // Arrange
        JobParameters parameters = new JobParametersBuilder()
                .addString("jobId", "123")
                .toJobParameters();

        when(jobOperator.start(any(Job.class), any(JobParameters.class)))
                .thenThrow(new RuntimeException("Test Exception"));

        // Act & Assert
        assertThrows(ExecuteJobException.class, () -> executor.execute(parameters));
        verify(jobOperator).start(uniProtImportJob, parameters);
    }
}

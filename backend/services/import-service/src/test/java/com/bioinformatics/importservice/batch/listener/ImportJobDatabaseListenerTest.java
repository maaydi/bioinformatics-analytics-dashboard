package com.bioinformatics.importservice.batch.listener;

import com.bioinformatics.importservice.dto.Constants;
import com.bioinformatics.importservice.dto.ImportStatus;
import com.bioinformatics.importservice.entity.ImportJob;
import com.bioinformatics.importservice.listener.ImportJobDatabaseListener;
import com.bioinformatics.importservice.repository.ImportJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportJobDatabaseListenerTest {

    @Mock
    private ImportJobRepository importJobRepository;

    private ImportJobDatabaseListener listener;
    private JobExecution jobExecution;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        listener = new ImportJobDatabaseListener(importJobRepository);
        jobId = UUID.randomUUID();

        JobParameters params = new JobParametersBuilder()
                .addString(Constants.IMPORT_JOB_ID.getKey(), jobId.toString())
                .toJobParameters();

        jobExecution = org.springframework.batch.test.MetaDataInstanceFactory.createJobExecution("job", 1L, 1L, params);
        jobExecution.setCreateTime(LocalDateTime.now());
        jobExecution.setEndTime(LocalDateTime.now().plusSeconds(5));
    }

    @Test
    void afterJob_withMissingJobId_returnsEarly() {
        jobExecution = org.springframework.batch.test.MetaDataInstanceFactory.createJobExecution("job", 1L, 1L, new JobParameters());

        listener.afterJob(jobExecution);

        verifyNoInteractions(importJobRepository);
    }

    @Test
    void afterJob_withNotFoundJob_returnsEarly() {
        when(importJobRepository.findById(jobId)).thenReturn(Optional.empty());

        listener.afterJob(jobExecution);

        verify(importJobRepository, never()).save(any());
    }

    @Test
    void afterJob_whenCompleted_updatesJobRecord() {
        ImportJob mockJob = new ImportJob();
        when(importJobRepository.findById(jobId)).thenReturn(Optional.of(mockJob));

        jobExecution.setStatus(BatchStatus.COMPLETED);

        StepExecution step1 = new StepExecution(0L, "step1", jobExecution);
        step1.setWriteCount(100);
        StepExecution step2 = new StepExecution(0L, "step2", jobExecution);
        step2.setWriteCount(50);

        jobExecution.addStepExecutions(java.util.List.of(step1, step2));

        listener.afterJob(jobExecution);

        ArgumentCaptor<ImportJob> captor = ArgumentCaptor.forClass(ImportJob.class);
        verify(importJobRepository).save(captor.capture());

        ImportJob saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ImportStatus.COMPLETED);
        assertThat(saved.getRecordsProcessed()).isEqualTo(150);
        assertThat(saved.getEntryCount()).isEqualTo(150);
        assertThat(saved.getDurationMs()).isGreaterThan(0);
        assertThat(saved.getCompletedAt()).isNotNull();
    }

    @Test
    void afterJob_whenFailed_extractsException_andUpdatesJobRecord() {
        ImportJob mockJob = new ImportJob();
        when(importJobRepository.findById(jobId)).thenReturn(Optional.of(mockJob));

        jobExecution.setStatus(BatchStatus.FAILED);
        jobExecution.addFailureException(new RuntimeException("Test failure reason"));

        listener.afterJob(jobExecution);

        ArgumentCaptor<ImportJob> captor = ArgumentCaptor.forClass(ImportJob.class);
        verify(importJobRepository).save(captor.capture());

        ImportJob saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ImportStatus.FAILED);
        assertThat(saved.getErrorMessage()).isEqualTo("Test failure reason");
    }

    @Test
    void afterJob_whenFailed_withNoExceptions_setsNoErrorMessage() {
        ImportJob mockJob = new ImportJob();
        when(importJobRepository.findById(jobId)).thenReturn(Optional.of(mockJob));

        jobExecution.setStatus(BatchStatus.FAILED);

        listener.afterJob(jobExecution);

        ArgumentCaptor<ImportJob> captor = ArgumentCaptor.forClass(ImportJob.class);
        verify(importJobRepository).save(captor.capture());

        ImportJob saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ImportStatus.FAILED);
        assertThat(saved.getErrorMessage()).isNull();
    }
}

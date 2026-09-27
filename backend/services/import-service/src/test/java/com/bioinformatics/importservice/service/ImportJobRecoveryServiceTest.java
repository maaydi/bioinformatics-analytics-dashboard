package com.bioinformatics.importservice.service;

import com.bioinformatics.importservice.dto.ImportStatus;
import com.bioinformatics.importservice.entity.ImportJob;
import com.bioinformatics.importservice.repository.ImportJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportJobRecoveryServiceTest {

    @Mock
    private ImportJobRepository importJobRepository;

    @InjectMocks
    private ImportJobRecoveryService service;

    @Test
    void markImportJobAsFailedUpdatesStatusWhenJobExists() {
        var jobId = UUID.randomUUID();
        var job = ImportJob.builder()
                .id(jobId)
                .status(ImportStatus.RUNNING)
                .build();

        when(importJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        service.markImportJobAsFailed(jobId);

        assertThat(job.getStatus()).isEqualTo(ImportStatus.FAILED);
        verify(importJobRepository).save(job);
    }

    @Test
    void markImportJobAsFailedDoesNothingWhenJobMissing() {
        var jobId = UUID.randomUUID();

        when(importJobRepository.findById(jobId)).thenReturn(Optional.empty());

        service.markImportJobAsFailed(jobId);

        verify(importJobRepository, never()).save(any());
    }
}


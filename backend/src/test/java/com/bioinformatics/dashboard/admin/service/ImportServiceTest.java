package com.bioinformatics.dashboard.admin.service;

import com.bioinformatics.dashboard.batch.AsyncUniprotImportJobExecutor;
import com.bioinformatics.dashboard.batch.counter.CounterRegistry;
import com.bioinformatics.dashboard.batch.counter.RecordCounter;
import com.bioinformatics.dashboard.config.AppProperties;
import com.bioinformatics.dashboard.exception.ImportAlreadyRunningException;
import com.bioinformatics.dashboard.job.dto.ImportJobProgress;
import com.bioinformatics.dashboard.job.dto.ImportJobSummary;
import com.bioinformatics.dashboard.job.dto.ImportStatus;
import com.bioinformatics.dashboard.job.entity.ImportJob;
import com.bioinformatics.dashboard.job.mapper.ImportJobMapper;
import com.bioinformatics.dashboard.job.repository.ImportJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    @Spy
    private final AppProperties appProperties = new AppProperties();
    @Mock
    private ImportJobRepository importJobRep;
    @Mock
    private ImportJobMapper jobMapper;
    @Mock
    private AsyncUniprotImportJobExecutor importExec;
    @Mock
    private CounterRegistry registry;
    @InjectMocks
    private ImportService importService;

    @Test
    void listImportJobs_returnsPagedSummary() {
        var jobEntity = ImportJob.builder()
                .id(UUID.randomUUID())
                .fileName("f1.txt")
                .status(ImportStatus.RUNNING)
                .totalEstimated(10)
                .build();

        when(importJobRep.findAll(PageRequest.of(0, 1, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))))
                .thenReturn(new PageImpl<>(List.of(jobEntity), PageRequest.of(0, 1), 1));

        var summary = new ImportJobSummary(jobEntity.getId().toString(), jobEntity.getStatus(), jobEntity.getFileName(), 0, 0, 0L, jobEntity.getCreatedAt(), jobEntity.getCompletedAt(), null);
        when(jobMapper.toSummary(any())).thenReturn(summary);

        var paged = importService.listImportJobs(0, 1);

        assertThat(paged).isNotNull();
        assertThat(paged.content()).hasSize(1);
        assertThat(paged.content().getFirst().id()).isEqualTo(jobEntity.getId().toString());
    }

    @Test
    void triggerImport_whenAnotherRunning_throws() {
        var runningJob = ImportJob.builder().id(UUID.randomUUID()).status(ImportStatus.RUNNING).build();
        when(importJobRep.findByStatus(ImportStatus.RUNNING)).thenReturn(List.of(runningJob));

        var file = new MockMultipartFile("file", "u.fasta", "text/plain", "seq".getBytes());

        assertThrows(ImportAlreadyRunningException.class, () -> importService.triggerImport(file, "overwrite"));

        verify(importJobRep, never()).save(any());
    }

    @Test
    void triggerImport_success_savesAndExecutes(@TempDir Path tempDir) throws Exception {
        appProperties.getImportConfig().setTempDir(tempDir.toString());

        when(importJobRep.findByStatus(ImportStatus.RUNNING)).thenReturn(List.of());

        RecordCounter counter = mock(RecordCounter.class);
        when(counter.count(any(InputStream.class))).thenReturn(5L);
        when(registry.getCounter(anyString())).thenReturn(counter);

        var inJob = new ImportJob();
        inJob.setId(UUID.randomUUID());
        inJob.setStatus(ImportStatus.RUNNING);
        inJob.setFileName("u.fasta");
        inJob.setTotalEstimated(5);

        when(importJobRep.save(any(ImportJob.class))).thenReturn(inJob);

        var summary = new ImportJobSummary(inJob.getId().toString(), inJob.getStatus(), inJob.getFileName(), 0, 0, 0L, inJob.getCreatedAt(), inJob.getCompletedAt(), null);
        when(jobMapper.toSummary(any())).thenReturn(summary);

        var file = new MockMultipartFile("file", "u.fasta", "text/plain", "AA\nBB\n".getBytes());

        var result = importService.triggerImport(file, "append");

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(inJob.getId().toString());

        verify(importExec, times(1)).execute(any());
        verify(importJobRep, times(1)).save(any(ImportJob.class));
    }

    @Test
    void getImportJobStatus_notFound_returnsFailedProgress() {
        var jobId = UUID.randomUUID().toString();
        when(importJobRep.findById(UUID.fromString(jobId))).thenReturn(Optional.empty());

        ImportJobProgress p = importService.getImportJobStatus(jobId);

        assertThat(p).isNotNull();
        assertThat(p.status()).isEqualTo(ImportStatus.FAILED);
        assertThat(p.errorMessage()).contains("Could not find job");
    }
}


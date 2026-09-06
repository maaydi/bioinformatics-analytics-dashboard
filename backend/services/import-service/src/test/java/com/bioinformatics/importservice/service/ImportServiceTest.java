package com.bioinformatics.importservice.service;

import com.bioinformatics.common.exception.ImportAlreadyRunningException;
import com.bioinformatics.common.exception.ResourceNotFoundException;
import com.bioinformatics.common.models.filter.SavedFilterDto;
import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.importservice.client.SavedFilterService;
import com.bioinformatics.importservice.config.ApplicationProperties;
import com.bioinformatics.importservice.dto.Constants;
import com.bioinformatics.importservice.dto.ImportJobSummary;
import com.bioinformatics.importservice.dto.ImportStatus;
import com.bioinformatics.importservice.entity.ImportJob;
import com.bioinformatics.importservice.mapper.ImportJobMapper;
import com.bioinformatics.importservice.repository.ImportJobRepository;
import com.bioinformatics.importservice.uniprot.apiloader.UniProtApiImportJobExecutor;
import com.bioinformatics.importservice.uniprot.fileloader.AsyncUniprotImportJobExecutor;
import com.bioinformatics.importservice.uniprot.fileloader.counter.CounterRegistry;
import com.bioinformatics.importservice.uniprot.fileloader.counter.RecordCounter;
import com.bioinformatics.shared.models.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    @Mock
    private ApplicationProperties appProperties;
    @Mock
    private ImportJobRepository importJobRep;
    @Mock
    private ImportJobMapper jobMapper;
    @Mock
    private AsyncUniprotImportJobExecutor importExec;
    @Mock
    private UniProtApiImportJobExecutor remoteImportExec;
    @Mock
    private CounterRegistry registry;
    @InjectMocks
    private ImportService importService;

    @Mock
    SavedFilterService savedFilterService;

    private UserPrincipal initiator;

    private static ApplicationProperties.ImportConfig importConfig(Path tempDir) {
        return new ApplicationProperties.ImportConfig(tempDir.toString(), List.of(), null);
    }

    @BeforeEach
    void setUp() {
        initiator = new UserPrincipal("user_test", List.of("ROLE_ADMIN"), "");
    }

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
        when(appProperties.importConfig()).thenReturn(importConfig(tempDir));

        when(importJobRep.findByStatus(ImportStatus.RUNNING)).thenReturn(List.of());

        var counter = mock(RecordCounter.class);
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
    void triggerRemoteImport_success_savesAndExecutes() {
        var filterId = 42L;
        when(importJobRep.findByStatus(ImportStatus.RUNNING)).thenReturn(List.of());

        var inJob = new ImportJob();
        inJob.setId(UUID.randomUUID());
        inJob.setStatus(ImportStatus.RUNNING);
        inJob.setFileName("UNIPROT_API_REMOTE");
        inJob.setStrategy("OVERWRITE");

        when(importJobRep.save(any(ImportJob.class))).thenReturn(inJob);

        var summary = new ImportJobSummary(inJob.getId().toString(), inJob.getStatus(), inJob.getFileName(), 0, 0, 0L, inJob.getCreatedAt(), inJob.getCompletedAt(), null);
        when(jobMapper.toSummary(any())).thenReturn(summary);
        when(savedFilterService.getSavedFilterById(anyLong(), any(UserPrincipal.class))).thenReturn(
                Optional.of(
                        new SavedFilterDto(42L, "example-filter", GeneSearchRequest.builder().accession("ACC").build(), Instant.now())
                )
        );
        var result = importService.triggerRemoteImport(filterId, initiator);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(inJob.getId().toString());

        var parametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(remoteImportExec, times(1)).execute(parametersCaptor.capture());
        assertThat(parametersCaptor.getValue().getLong(Constants.SAVED_FILTER_ID.getKey()))
                .isEqualTo(filterId);
        verify(importJobRep, times(1)).save(any(ImportJob.class));
    }

    @Test
    void triggerRemoteImport_whenAnotherRunning_throws() {
        var filterId = 42L;
        var runningJob = ImportJob.builder().id(UUID.randomUUID()).status(ImportStatus.RUNNING).build();
        when(importJobRep.findByStatus(ImportStatus.RUNNING)).thenReturn(List.of(runningJob));

        assertThrows(ImportAlreadyRunningException.class, () -> importService.triggerRemoteImport(filterId, initiator));

        verify(importJobRep, never()).save(any());
        verify(remoteImportExec, never()).execute(any());
    }

    @Test
    void getImportJobStatus_notFound_returnsFailedProgress() {
        var jobId = UUID.randomUUID().toString();
        when(importJobRep.findById(UUID.fromString(jobId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> importService.getImportJobStatus(jobId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Import job not found:")
                .hasMessageContaining(jobId);
    }
}

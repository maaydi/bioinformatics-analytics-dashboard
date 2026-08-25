package com.bioinformatics.importservice.batch.listener;

import com.bioinformatics.common.gene.entity.ProteinEntry;
import com.bioinformatics.importservice.entity.ImportJob;
import com.bioinformatics.importservice.listener.ImportProgressChunkListener;
import com.bioinformatics.importservice.repository.ImportJobRepository;
import com.bioinformatics.importservice.uniprot.fileloader.UniProtImportJobParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportProgressChunkListenerTest {

    private final UUID jobId = UUID.randomUUID();
    @Mock
    private ImportJobRepository repository;
    @Mock
    private UniProtImportJobParameters jobParameters;
    private ImportProgressChunkListener listener;

    @BeforeEach
    void setUp() {
        listener = new ImportProgressChunkListener(repository, jobParameters);
        when(jobParameters.getJobId()).thenReturn(jobId.toString());
    }

    @Test
    void afterChunk_updatesProcessedRecords() {
        ImportJob mockJob = new ImportJob();
        mockJob.setRecordsProcessed(100);

        when(repository.findById(jobId)).thenReturn(Optional.of(mockJob));
        when(repository.save(any(ImportJob.class))).thenAnswer(i -> i.getArgument(0));

        Chunk<ProteinEntry> chunk = new Chunk<>();
        chunk.add(new ProteinEntry());
        chunk.add(new ProteinEntry());
        chunk.add(new ProteinEntry());
        // chunk size = 3

        listener.afterChunk(chunk);

        ArgumentCaptor<ImportJob> captor = ArgumentCaptor.forClass(ImportJob.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getRecordsProcessed()).isEqualTo(103);
    }

    @Test
    void afterChunk_whenJobNotFound_returnsEarly() {
        when(repository.findById(jobId)).thenReturn(Optional.empty());

        Chunk<ProteinEntry> chunk = new Chunk<>();
        chunk.add(new ProteinEntry());

        listener.afterChunk(chunk);

        verify(repository, never()).save(any());
    }
}


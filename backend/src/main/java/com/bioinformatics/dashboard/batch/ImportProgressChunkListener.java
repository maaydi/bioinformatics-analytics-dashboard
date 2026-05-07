package com.bioinformatics.dashboard.batch;

import com.bioinformatics.dashboard.gene.entity.ProteinEntry;
import com.bioinformatics.dashboard.job.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImportProgressChunkListener implements ChunkListener<String, ProteinEntry> {
    private final ImportJobRepository repository;
    private final UniProtImportJobParameters jobParameters;

    @Override
    public void afterChunk(@NonNull Chunk<ProteinEntry> chunk) {
        var jobId = UUID.fromString(jobParameters.getJobId());
        log.info("Update processed records for Job <{}>", jobId);
        var job = repository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("Job <{}> not found", jobId);
            return;
        }
        var current = job.getRecordsProcessed();
        job.setRecordsProcessed(current + chunk.size());
        var saved = repository.save(job);
        log.info("Updated records for Job <{}> : Records processed = {}", jobId, saved.getRecordsProcessed());
    }


}

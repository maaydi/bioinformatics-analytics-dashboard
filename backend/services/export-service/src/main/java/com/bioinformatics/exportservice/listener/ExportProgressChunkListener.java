package com.bioinformatics.exportservice.listener;

import com.bioinformatics.common.gene.dto.ProteinDetailDto;
import com.bioinformatics.exportservice.batch.ExportJobParameters;
import com.bioinformatics.exportservice.repository.ExportJobExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExportProgressChunkListener implements ChunkListener<String, ProteinDetailDto> {
    /**
     * Updates export job progress in the database after each processed chunk.
     * Keeps the ExportJob.chunksProcessed counter up-to-date for monitoring.
     */
    private final ExportJobExecutionRepository repository;
    private final ExportJobParameters jobParameters;

    @Override
    public void afterChunk(@NonNull Chunk<ProteinDetailDto> chunk) {
        var jobId = jobParameters.getJobId();
        log.info("Update processed chunks for Job <{}>", jobId);
        var job = repository.findByPipelineId(jobId).orElse(null);
        if (job == null) {
            log.warn("Job <{}> not found", jobId);
            return;
        }
        var current = job.getChunksProcessed();
        job.setChunksProcessed(current + chunk.size());
        var saved = repository.save(job);
        log.info("Updated chunks for Job <{}> : Chunks processed = {}", jobId, saved.getChunksProcessed());
    }


}
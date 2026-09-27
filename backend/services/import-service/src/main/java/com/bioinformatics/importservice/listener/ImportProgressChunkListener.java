package com.bioinformatics.importservice.listener;

import com.bioinformatics.common.gene.entity.ProteinEntry;
import com.bioinformatics.importservice.repository.ImportJobRepository;
import com.bioinformatics.importservice.uniprot.ImportJobParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Batch chunk listener for tracking import progress in real-time.
 *
 * <p>Updates ImportJob progress metrics after each chunk completes processing.
 * Enables UI to display real-time progress bars and processing statistics.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Chunk reader fetches items from source (file or API)
 *   <li>Chunk processor transforms items to protein entries
 *   <li>Chunk writer persists to database
 *   <li>{@code afterChunk} increments recordsProcessed counter
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImportProgressChunkListener implements ChunkListener<String, ProteinEntry> {

    private final ImportJobRepository repository;
    private final ImportJobParameters jobParameters;

    /**
     * Called after each chunk is successfully processed and written to database.
     *
     * <p>Updates ImportJob.recordsProcessed counter and logs progress.
     * This counter is polled by UI to display progress percentage.
     *
     * @param chunk successfully processed chunk of protein entries
     */
    @Override
    public void afterChunk(@NonNull Chunk<ProteinEntry> chunk) {
        var jobId = UUID.fromString(jobParameters.getJobId());
        log.debug("[CHUNK_LISTENER] Chunk processing complete - pipelineId={}, chunkSize={}",
                jobId, chunk.size());
        
        var job = repository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("[CHUNK_LISTENER] Import job not found in database - ID={}", jobId);
            return;
        }

        var previous = job.getRecordsProcessed();
        job.setRecordsProcessed(previous + chunk.size());
        var saved = repository.save(job);

        var progress = saved.getTotalEstimated() > 0
                ? (saved.getRecordsProcessed() * 100) / saved.getTotalEstimated()
                : 0;

        log.info("[CHUNK_LISTENER] Progress updated - ID={}, records_processed={}/{}, progress={}%",
                jobId, saved.getRecordsProcessed(), saved.getTotalEstimated(), progress);
    }


}

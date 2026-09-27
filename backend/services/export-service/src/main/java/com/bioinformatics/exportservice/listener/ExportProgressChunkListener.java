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

/**
 * Batch chunk listener for tracking export progress.
 *
 * <p>Updates ExportJobExecution progress metrics after each chunk
 * completes processing. Enables UI to display real-time progress bars.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Chunk reader fetches items
 *   <li>Chunk processor transforms items
 *   <li>Chunk writer persists segment file
 *   <li>{@code afterChunk} increments chunksProcessed counter
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExportProgressChunkListener implements ChunkListener<String, ProteinDetailDto> {

    /**
     * Repository for updating chunk progress counters.
     */
    private final ExportJobExecutionRepository repository;

    /**
     * Job parameters holder (provides pipeline ID and other context).
     */
    private final ExportJobParameters jobParameters;

    /**
     * Called after each chunk is successfully processed and written.
     *
     * <p>Updates {@code ExportJobExecution.chunksProcessed} to track progress.
     * This counter is used by UI to display progress percentage.
     *
     * @param chunk successfully processed chunk
     */
    @Override
    public void afterChunk(@NonNull Chunk<ProteinDetailDto> chunk) {
        var jobId = jobParameters.getJobId();
        log.debug("[CHUNK_LISTENER] Chunk processing complete - pipelineId={}, chunkSize={}",
                jobId, chunk.size());

        repository.findByPipelineId(jobId)
                .ifPresentOrElse(job -> {
                    var previous = job.getChunksProcessed();
                    job.setChunksProcessed(previous + 1);
                    var saved = repository.save(job);

                    var progress = saved.getProgressPercent();
                    log.info("[CHUNK_LISTENER] Progress updated - pipelineId={}, chunks_processed={}/{}, progress={}%",
                            jobId, saved.getChunksProcessed(), saved.getChunksTotal(), progress);

                }, () -> log.warn("[CHUNK_LISTENER] Job execution not found - pipelineId={}", jobId));
    }
}
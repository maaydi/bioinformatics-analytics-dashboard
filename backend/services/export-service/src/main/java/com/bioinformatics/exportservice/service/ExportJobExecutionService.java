package com.bioinformatics.exportservice.service;

import com.bioinformatics.common.exception.ResourceNotFoundException;
import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.exportservice.entity.ExportJobExecution;
import com.bioinformatics.exportservice.repository.ExportJobExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Provides persistence and lookup operations for export job execution records.
 *
 * <p>The execution record links a Spring Batch job execution to its originating export
 * pipeline and keeps access to the stored filter payload used to reproduce the export.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ExportJobExecutionService {
    private final ExportJobExecutionRepository exportJobExecutionRepository;

    /**
     * Finds the execution metadata for the supplied export pipeline.
     *
     * @param pipelineId identifier of the export pipeline
     * @return an optional execution record when one exists
     */
    public Optional<ExportJobExecution> findByPipelineId(Long pipelineId) {
        log.debug("[EXPORT][JOB_EXECUTION] Looking up execution metadata for pipelineId={}", pipelineId);

        var execution = exportJobExecutionRepository.findByPipelineId(pipelineId);
        if (execution.isPresent()) {
            var jobExecution = execution.get();
            log.debug("[EXPORT][JOB_EXECUTION] Found execution for pipelineId={} executionId={}",
                    pipelineId, jobExecution.getId());
            return Optional.of(jobExecution);
        }

        log.debug("[EXPORT][JOB_EXECUTION] No execution metadata found for pipelineId={}", pipelineId);
        return Optional.empty();
    }

    /**
     * Resolves the original gene search criteria associated with a pipeline.
     *
     * @param pipelineId identifier of the pipeline whose filter should be retrieved
     * @return the persisted {@link GeneSearchRequest} used when the export was created
     * @throws ResourceNotFoundException when the pipeline execution metadata cannot be found
     */
    public GeneSearchRequest getPipelineRequest(Long pipelineId) {
        log.debug("[EXPORT][JOB_EXECUTION] Resolving pipeline request for pipelineId={}", pipelineId);

        var execution = exportJobExecutionRepository.findByPipelineId(pipelineId);
        if (execution.isPresent()) {
            var request = execution.get().getPipeline().getFilterJson();
            log.debug("[EXPORT][JOB_EXECUTION] Resolved pipeline request for pipelineId={} filter={}",
                    pipelineId, request);
            return request;
        }

        log.warn("[EXPORT][JOB_EXECUTION] Pipeline request lookup failed because no execution exists for pipelineId={}",
                pipelineId);
        throw new ResourceNotFoundException("Pipeline not found");
    }

    /**
     * Saves a new or updated export job execution record.
     *
     * @param exportJobExecution execution entity to persist
     * @return persisted execution entity
     */
    public ExportJobExecution save(ExportJobExecution exportJobExecution) {
        log.debug("[EXPORT][JOB_EXECUTION] Saving export job execution={}", exportJobExecution);

        var saved = exportJobExecutionRepository.save(exportJobExecution);
        log.info("[EXPORT][JOB_EXECUTION] Saved export job execution id={} pipelineId={}",
                saved.getId(), saved.getPipeline() != null ? saved.getPipeline().getId() : null);
        return saved;
    }
}

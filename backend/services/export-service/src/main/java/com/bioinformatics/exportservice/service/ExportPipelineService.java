package com.bioinformatics.exportservice.service;

import com.bioinformatics.exportservice.dto.ExportStatus;
import com.bioinformatics.exportservice.repository.ExportPipelineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportPipelineService {

    private final ExportPipelineRepository pipelineRepository;

    public void markAsCompleted(Long pipelineId, String finalFile, long fileSizeBytes, long actualRows) {
        log.info("Update Pipeline Status to COMPLETED");
        pipelineRepository.findById(pipelineId).ifPresentOrElse(pipeline -> {
            pipeline.setStatus(ExportStatus.COMPLETED);
            pipeline.setFileSizeBytes(fileSizeBytes);
            pipeline.setActualRows(actualRows);
            pipeline.setFilePath(finalFile);
            pipelineRepository.save(pipeline);
            log.info("Pipeline updated successfully");
        }, () -> logFailedUpdate(pipelineId));

    }

    public void markAsFailed(Long pipelineId) {
        log.info("Update Pipeline Status to FAILED");
        pipelineRepository.findById(pipelineId).ifPresentOrElse(pipeline -> {
            pipeline.setStatus(ExportStatus.FAILED);
            pipelineRepository.save(pipeline);
        }, () -> logFailedUpdate(pipelineId));
    }

    public void startPipeline(Long pipelineId) {
        pipelineRepository.findById(pipelineId).ifPresentOrElse(pipeline -> {
            pipeline.setStatus(ExportStatus.RUNNING);
            pipeline.setStartedAt(Instant.now());
            pipelineRepository.save(pipeline);
        }, () -> log.error("Failed to Start Pipeline {}: Pipeline not Found", pipelineId));
    }

    private void logFailedUpdate(long pipelineId) {
        log.error("Failed to update Pipeline: Pipeline with ID <{}> not found", pipelineId);
    }
}

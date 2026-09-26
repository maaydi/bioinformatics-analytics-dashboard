package com.bioinformatics.exportservice.service;

import com.bioinformatics.exportservice.config.ApplicationProperties;
import com.bioinformatics.exportservice.entity.ExportPipeline;
import com.bioinformatics.exportservice.repository.ExportJobExecutionRepository;
import com.bioinformatics.exportservice.repository.ExportPipelineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExportCleanupJob {

    private final ExportFileStorageService storageService;
    private final ApplicationProperties applicationProperties;
    private final ExportPipelineRepository exportPipelineRepository;
    private final ExportJobExecutionRepository exportJobExecutionRepository;
    private final JobRepository jobRepository;

    @Scheduled(fixedRateString = "${app.export.cleanup.retention-days:1}", timeUnit = TimeUnit.DAYS)
    public void cleanDeletedPipelineFiles() {
        var days = applicationProperties.export().cleanup().retentionDays();
        log.info("Cleaning up Deleted Pipeline files that were deleted last {} days", days);
        var before = Instant.now();
        var after = before.minus(days, ChronoUnit.DAYS);
        var pipelines = exportPipelineRepository.findAllByDeletedAtBetween(after, before);
        pipelines
                .forEach(pipeline -> {
                    log.info("Delete files for pipeline '{}' [ID={}, DeletedAt={}]", pipeline.getName(), pipeline.getId(), pipeline.getDeletedAt());
                    try {
                        storageService.deletePipelineDirectory(pipeline.getUserId(), pipeline.getId());
                        jobRepository.deleteJobExecution(Objects.requireNonNull(jobRepository.getJobExecution(pipeline.getJobExecutionId())));
                    } catch (IOException e) {
                        log.warn("Failed to clean deleted pipeline '{}' files: {}", pipeline.getName(), e.getMessage());
                    } catch (NullPointerException e) {
                        log.warn("Failed to clean deleted pipeline '{}' job execution", pipeline.getName());
                    } catch (Exception e) {
                        log.warn("Un error occurs while cleaning deleted pipeline '{}': {}", pipeline.getName(), e.getMessage());
                    }
                });
        log.info("Delete DB records in Pipeline and JobExecution entities");
        var ids = pipelines.stream().map(ExportPipeline::getId).toList();
        exportPipelineRepository.deleteAllByIdInBatch(ids);
        exportJobExecutionRepository.deleteByPipelineIdIn(ids);
        log.info("Cleaning up Deleted Pipeline records and files that were deleted last {} days is DONE", days);

    }

}

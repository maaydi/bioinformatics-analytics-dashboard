package com.bioinformatics.exportservice.service;

import com.bioinformatics.exportservice.config.ApplicationProperties;
import com.bioinformatics.exportservice.repository.ExportPipelineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class PipelineCleanupTask {

    private final ExportFileStorageService storageService;
    private final ApplicationProperties applicationProperties;
    private final ExportPipelineRepository exportPipelineRepository;

    @Scheduled(fixedRateString = "${app.export.cleanup.retention-days:1}", timeUnit = TimeUnit.DAYS)
    public void cleanDeletedPipelineFiles() {
        var days = applicationProperties.export().cleanup().retentionDays();
        log.info("Cleaning up Deleted Pipeline files that were deleted last {} days", days);
        var before = Instant.now();
        var after = before.minus(days, ChronoUnit.DAYS);
        exportPipelineRepository.findAllByDeletedAtBetween(after, before)
                .forEach(pipeline -> {
                    log.info("Delete files for pipeline '{}' [ID={}, DeletedAt={}]", pipeline.getName(), pipeline.getId(), pipeline.getDeletedAt());
                    try {
                        storageService.deletePipelineDirectory(pipeline.getUserId(), pipeline.getId());
                    } catch (IOException e) {
                        log.warn("Failed to clean deleted pipeline '{}' files: {}", pipeline.getName(), e.getMessage());
                    }
                });
        log.info("Cleaning up Deleted Pipeline files that were deleted last {} days is DONE", days);

    }

}

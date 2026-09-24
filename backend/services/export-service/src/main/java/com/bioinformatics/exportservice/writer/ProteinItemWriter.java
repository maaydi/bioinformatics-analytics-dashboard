package com.bioinformatics.exportservice.writer;

import com.bioinformatics.exportservice.dto.ExportFormat;
import com.bioinformatics.exportservice.service.ExportFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;


import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class ProteinItemWriter implements ItemWriter<Map<String, Object>> {

    private final ExportFormatWriter formatWriter;
    private final ExportFileStorageService storageService;
    private final String userId;
    private final Long pipelineId;
    private final ExportFormat format;
    private final List<String> fields;

    private final AtomicInteger chunkCounter = new AtomicInteger(0);

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        try {
            storageService.createPipelineDirectory(userId, pipelineId);
            log.info("Initialized pipeline directory for userId: {}, pipelineId: {}", userId, pipelineId);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize pipeline directory", e);
        }
    }

    @Override
    public void write(Chunk<? extends Map<String, Object>> chunk) throws Exception {
        if (chunk.isEmpty()) {
            return;
        }

        int currentChunk = chunkCounter.incrementAndGet();
        Path segmentPath = storageService.getSegmentPath(userId, pipelineId, currentChunk, format);

        log.debug("Writing chunk {} to segment {}", currentChunk, segmentPath);

        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(segmentPath))) {
            formatWriter.writeHeader(fields, out);

            for (Map<String, Object> item : chunk) {
                formatWriter.writeRow(item, fields, out);
            }

            formatWriter.close(out);
        }
    }
}
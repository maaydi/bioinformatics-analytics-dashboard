package com.bioinformatics.exportservice.writer;

import com.bioinformatics.exportservice.dto.ExportFormat;
import com.bioinformatics.exportservice.service.ExportFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStream;
import org.springframework.batch.infrastructure.item.ItemWriter;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ExportItemWriter implements ItemWriter<Map<String, Object>>, ItemStream {

    private static final String SEGMENT_INDEX_CONTEXT_KEY = "export.segment-index";

    private final ExportFormatWriter formatWriter;
    private final ExportFileStorageService storageService;
    private final String userId;
    private final Long pipelineId;
    private final ExportFormat format;
    private final List<String> fields;

    private int segmentIndex;

    @Override
    public void open(ExecutionContext executionContext) {
        try {
            storageService.createPipelineDirectory(userId, pipelineId);
            segmentIndex = executionContext.getInt(SEGMENT_INDEX_CONTEXT_KEY, 0);
            advancePastExistingSegments();
            log.info("Initialized export writer for userId: {}, pipelineId: {}, next segment: {}",
                    userId, pipelineId, segmentIndex + 1);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize export writer", e);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putInt(SEGMENT_INDEX_CONTEXT_KEY, segmentIndex);
    }

    @Override
    public void close() {
        // Each chunk writer is finalized within write; no step-scoped resource remains open.
    }

    @Override
    public void write(Chunk<? extends Map<String, Object>> chunk) throws Exception {
        if (chunk.isEmpty()) {
            return;
        }

        int currentChunk = ++segmentIndex;
        Path segmentPath = storageService.getSegmentPath(userId, pipelineId, currentChunk, format);

        log.debug("Writing chunk {} to segment {}", currentChunk, segmentPath);

        OutputStream out = new BufferedOutputStream(Files.newOutputStream(segmentPath));
        try {
            formatWriter.writeHeader(fields, out);

            for (Map<String, Object> item : chunk) {
                formatWriter.writeRow(item, fields, out);
            }

            formatWriter.close(out);
        } catch (Exception exception) {
            try (out) {
                formatWriter.close(out);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            Files.deleteIfExists(segmentPath);
            segmentIndex--;
            throw exception;
        } finally {
            out.close();
        }
    }

    private void advancePastExistingSegments() throws IOException {
        while (Files.exists(storageService.getSegmentPath(userId, pipelineId, segmentIndex + 1, format))) {
            segmentIndex++;
        }
    }
}
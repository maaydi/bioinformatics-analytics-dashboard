package com.bioinformatics.exportservice.service;

import com.bioinformatics.exportservice.dto.ExportFormat;

import java.io.IOException;
import java.nio.file.Path;

public interface ExportFileStorageService {

    Path createPipelineDirectory(Long userId, Long pipelineId) throws IOException;

    Path getSegmentPath(Long userId, Long pipelineId, int chunkNumber, ExportFormat format);

    Path getFinalFilePath(Long userId, Long pipelineId, ExportFormat format);

    /**
     * Assemble segments into a final file. Returns path to assembled final file.
     */
    Path assembleSegments(Long userId, Long pipelineId, ExportFormat format) throws IOException;

    void deletePipelineDirectory(Long userId, Long pipelineId) throws IOException;

    long getFileSize(Long userId, Long pipelineId, ExportFormat format) throws IOException;

    boolean validateFileExists(Long userId, Long pipelineId, ExportFormat format);
}


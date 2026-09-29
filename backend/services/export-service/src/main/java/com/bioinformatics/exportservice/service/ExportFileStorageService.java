package com.bioinformatics.exportservice.service;

import com.bioinformatics.exportservice.dto.ExportFormat;

import java.io.IOException;
import java.nio.file.Path;

public interface ExportFileStorageService {

    Path createPipelineDirectory(String userId, Long pipelineId) throws IOException;

    Path getSegmentPath(String userId, Long pipelineId, int chunkNumber, ExportFormat format);

    Path getFinalFilePath(String userId, Long pipelineId, ExportFormat format);

    /**
     * Assemble segments into a final file. Returns path to assembled final file.
     */
    Path assembleSegments(String userId, Long pipelineId, ExportFormat format) throws IOException;

    void deletePipelineDirectory(String userId, Long pipelineId) throws IOException;

    long getFileSize(String userId, Long pipelineId, ExportFormat format) throws IOException;

    boolean validateFileExists(String userId, Long pipelineId, ExportFormat format);
}


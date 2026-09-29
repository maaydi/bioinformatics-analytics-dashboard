package com.bioinformatics.exportservice.dto;

/**
 * DTO for providing download URL and file metadata.
 *
 * <p>Used by the `/api/exports/pipelines/{id}/download` endpoint.
 * Provides direct download link and file information.
 */
public record DownloadUrlDto(
        String downloadUrl,
        String filename,
        Long fileSizeBytes,
        String contentType
) {
}


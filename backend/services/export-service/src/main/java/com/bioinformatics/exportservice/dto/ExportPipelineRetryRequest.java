package com.bioinformatics.exportservice.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for retrying a failed export pipeline.
 *
 * <p>Used by the `/api/exports/pipelines/{id}/retry` endpoint.
 * Re-runs the pipeline with the same configuration.
 */
public record ExportPipelineRetryRequest(
        @NotNull(message = "Pipeline ID cannot be null")
        Long pipelineId
) {
}


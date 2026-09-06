package com.bioinformatics.exportservice.dto;

import com.bioinformatics.common.models.gene.GeneSearchRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO for creating a new export pipeline.
 *
 * <p>Accepts:
 * <ul>
 *   <li>Pipeline name and optional description
 *   <li>Filter criteria (as JsonNode for flexibility with various filter formats)
 *   <li>Export format (CSV, TSV, JSON, EXCEL)
 *   <li>Ordered list of field names to export
 * </ul>
 *
 * <p>All fields are validated using Bean Validation annotations.
 */
public record ExportPipelineCreateRequest(
        @NotBlank(message = "Pipeline name cannot be blank")
        @Size(max = 200, message = "Pipeline name must be at most 200 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @NotNull(message = "Filter criteria cannot be null")
        GeneSearchRequest filter,

        @NotNull(message = "Export format cannot be null")
        ExportFormat format,

        @NotEmpty(message = "At least one field must be selected for export")
        @Size(max = 50, message = "Maximum 50 fields can be selected")
        List<@NotBlank(message = "Field name cannot be blank") String> fieldSchema
) {
}



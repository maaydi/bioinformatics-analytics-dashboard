package com.bioinformatics.exportservice.dto;

/**
 * DTO describing an exportable field for the field picker UI.
 *
 * <p>Used by the `/api/exports/fields` endpoint to populate the field picker
 * in the export wizard. Includes display name, data type hints, and description
 * for user guidance.
 */
public record ExportFieldSchemaDto(
        String fieldName,
        String displayName,
        String dataType,
        String description,
        boolean available
) {
}


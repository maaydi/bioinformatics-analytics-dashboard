package com.bioinformatics.common.gene.dto;

import com.bioinformatics.shared.models.gene.ExportFormatSerializable;
import com.bioinformatics.shared.models.gene.export.ExportField;
import com.bioinformatics.shared.models.gene.export.ExportIgnore;
import lombok.Builder;

import java.util.Map;

/**
 * Protein keyword/tag record used in protein detail and analytics aggregations.
 */
@Builder
public record KeywordDto(
        @ExportIgnore
        int id,
        @ExportField(name = "name", displayName = "Keyword", description = "Controlled vocabulary term assigned to the protein")
        String name
) implements ExportFormatSerializable {
    @Override
    public Map<String, Object> row() {
        return Map.of("name", format(name));
    }

}
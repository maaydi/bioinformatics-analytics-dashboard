package com.bioinformatics.common.gene.dto;

import com.bioinformatics.shared.models.gene.ExportFormatSerializable;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Protein keyword/tag record used in protein detail and analytics aggregations.
 */
@Builder
public record KeywordDto(int id, String name) implements ExportFormatSerializable {
    @Override
    public Map<String, Object> row() {
        return Map.of("name", format(name));
    }

    @Override
    public List<String> fieldsExcluded() {
        return List.of("id");
    }
}
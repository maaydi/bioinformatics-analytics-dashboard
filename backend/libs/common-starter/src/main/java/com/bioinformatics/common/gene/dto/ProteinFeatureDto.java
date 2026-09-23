package com.bioinformatics.common.gene.dto;

import com.bioinformatics.shared.models.gene.ExportFormatSerializable;
import lombok.Builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Protein feature record indicating a functionally significant region or domain within the sequence.
 * Examples: transmembrane domain, signal peptide, active site, zinc finger.
 */
@Builder
public record ProteinFeatureDto(long id, String featureType, int startPos, int endPos, String note, String featureId,
                                String evidence) implements ExportFormatSerializable {
    @Override
    public List<String> fieldsExcluded() {
        return List.of("id");
    }

    @Override
    public Map<String, Object> row() {
        return new HashMap<>() {{
            put("FeatureType", format(featureType));
            put("Position", format("(%d,%d)".formatted(startPos, endPos)));
            put("Note", format(note));
            put("FeatureId", format(featureId));
            put("Evidence", format(evidence));
        }};
    }
}
package com.bioinformatics.common.gene.dto;

import com.bioinformatics.shared.models.gene.ExportFormatSerializable;
import com.bioinformatics.shared.models.gene.export.ExportField;
import com.bioinformatics.shared.models.gene.export.ExportIgnore;
import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

/**
 * Protein feature record indicating a functionally significant region or domain within the sequence.
 * Examples: transmembrane domain, signal peptide, active site, zinc finger.
 */
@Builder
public record ProteinFeatureDto(
        @ExportIgnore
        long id,
        @ExportField(name = "featureType", displayName = "Feature Type", description = "Type of annotated feature (e.g., CHAIN, DOMAIN, SIGNAL, BINDING)")
        String featureType,
        @ExportField(name = "startPos", displayName = "Start Position", description = "Start position of the feature in the protein sequence")
        int startPos,
        @ExportField(name = "endPos", displayName = "End Position", description = "End position of the feature in the protein sequence")
        int endPos,
        @ExportField(name = "note", displayName = "Note", description = "Additional description or annotation for the feature")
        String note,
        @ExportField(name = "featureId", displayName = "Feature ID", description = "Unique identifier for the feature")
        String featureId,
        @ExportField(name = "evidence", displayName = "Evidence", description = "Evidence code supporting the feature annotation")
        String evidence
) implements ExportFormatSerializable {
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
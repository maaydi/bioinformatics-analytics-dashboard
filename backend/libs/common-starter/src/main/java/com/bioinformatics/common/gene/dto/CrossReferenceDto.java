package com.bioinformatics.common.gene.dto;

import com.bioinformatics.shared.models.gene.ExportFormatSerializable;
import lombok.Builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * External database cross-reference record used in protein detail responses.
 * Links proteins to identifiers in other databases (e.g., UniProt, InterPro, PDB).
 */
@Builder
public record CrossReferenceDto(long id, String source, String identifier, String secondaryId,
                                String tertiaryInfo) implements ExportFormatSerializable {
    @Override
    public Map<String, Object> row() {
        return new HashMap<>() {{
            put("source", format(source));
            put("identifier", format(identifier));
            put("secondaryId", format(secondaryId));
            put("tertiaryInfo", format(tertiaryInfo));
        }};
    }

    @Override
    public List<String> fieldsExcluded() {
        return List.of("id");
    }
}
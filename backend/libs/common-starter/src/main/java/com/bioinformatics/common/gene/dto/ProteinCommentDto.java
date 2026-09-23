package com.bioinformatics.common.gene.dto;

import com.bioinformatics.shared.models.gene.ExportFormatSerializable;
import lombok.Builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Protein comment record containing annotations and notes about the protein.
 * Examples: catalytic activity, function, subcellular location, disease association.
 */
@Builder
public record ProteinCommentDto(long id, String commentType, String text) implements ExportFormatSerializable {
    @Override
    public Map<String, Object> row() {
        return new HashMap<>() {{
            put("commentType", format(commentType));
            put("text", format(text));
        }};
    }

    @Override
    public List<String> fieldsExcluded() {
        return List.of("id");
    }
}
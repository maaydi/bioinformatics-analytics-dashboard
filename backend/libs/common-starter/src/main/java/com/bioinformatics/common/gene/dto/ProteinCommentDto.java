package com.bioinformatics.common.gene.dto;

import com.bioinformatics.shared.models.gene.ExportFormatSerializable;
import com.bioinformatics.shared.models.gene.export.ExportField;
import com.bioinformatics.shared.models.gene.export.ExportIgnore;
import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

/**
 * Protein comment record containing annotations and notes about the protein.
 * Examples: catalytic activity, function, subcellular location, disease association.
 */
@Builder
public record ProteinCommentDto(
        @ExportIgnore
        long id,
        @ExportField(name = "commentType", displayName = "Comment Type", description = "Category of comment (e.g., FUNCTION, CATALYTIC_ACTIVITY, SUBCELLULAR_LOCATION, DISEASE)")
        String commentType,
        @ExportField(name = "text", displayName = "Comment Text", description = "Annotated comment text with evidence codes and cross-references")
        String text
) implements ExportFormatSerializable {
    @Override
    public Map<String, Object> row() {
        return new HashMap<>() {{
            put("commentType", format(commentType));
            put("text", format(text));
        }};
    }

}
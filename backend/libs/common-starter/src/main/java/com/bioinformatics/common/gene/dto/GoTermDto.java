package com.bioinformatics.common.gene.dto;

import com.bioinformatics.shared.models.gene.ExportFormatSerializable;
import com.bioinformatics.shared.models.gene.export.ExportField;
import com.bioinformatics.shared.models.gene.export.ExportIgnore;
import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

/**
 * Gene Ontology (GO) term record used in protein detail responses.
 * Represents a GO classification (Process, Function, or Component) assigned to a protein.
 */
@Builder
public record GoTermDto(
        @ExportIgnore
        int id,
        @ExportField(name = "goId", displayName = "GO ID", description = "Gene Ontology term identifier (e.g., GO:0046782)")
        String goId,
        @ExportField(name = "aspect", displayName = "GO Aspect", description = "Branch of Gene Ontology: P (Biological Process), F (Molecular Function), or C (Cellular Component)")
        Character aspect,
        @ExportField(name = "description", displayName = "GO Description", description = "Human-readable description of the Gene Ontology term")
        String description
) implements ExportFormatSerializable {
    @Override
    public Map<String, Object> row() {
        return new HashMap<>() {{
            put("goId", format(goId));
            put("aspect", aspect);
            put("description", format(description));
        }};
    }

}
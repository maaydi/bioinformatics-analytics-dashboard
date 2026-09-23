package com.bioinformatics.common.gene.dto;

import com.bioinformatics.shared.models.gene.ExportFormatSerializable;
import lombok.Builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gene Ontology (GO) term record used in protein detail responses.
 * Represents a GO classification (Process, Function, or Component) assigned to a protein.
 */
@Builder
public record GoTermDto(int id, String goId, Character aspect, String description) implements ExportFormatSerializable {
    @Override
    public Map<String, Object> row() {
        return new HashMap<>() {{
            put("goId", format(goId));
            put("aspect", aspect);
            put("description", format(description));
        }};
    }

    @Override
    public List<String> fieldsExcluded() {
        return List.of("id");
    }
}
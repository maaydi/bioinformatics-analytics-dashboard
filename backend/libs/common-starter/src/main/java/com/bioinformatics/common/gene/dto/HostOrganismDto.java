package com.bioinformatics.common.gene.dto;

import com.bioinformatics.shared.models.gene.ExportFormatSerializable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Host organism record for viral or pathogenic proteins.
 * Records the organism(s) that a protein infects or is associated with.
 */
public record HostOrganismDto(long id, int taxid, String name) implements ExportFormatSerializable {
    @Override
    public Map<String, Object> row() {
        return new HashMap<>() {{
            put("taxid", format(taxid));
            put("name", format(name));
        }};
    }

    @Override
    public List<String> fieldsExcluded() {
        return List.of("id");
    }
}
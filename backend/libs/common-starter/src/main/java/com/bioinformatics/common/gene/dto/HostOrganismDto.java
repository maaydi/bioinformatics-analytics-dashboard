package com.bioinformatics.common.gene.dto;

import com.bioinformatics.shared.models.gene.ExportFormatSerializable;
import com.bioinformatics.shared.models.gene.export.ExportField;
import com.bioinformatics.shared.models.gene.export.ExportIgnore;

import java.util.HashMap;
import java.util.Map;

/**
 * Host organism record for viral or pathogenic proteins.
 * Records the organism(s) that a protein infects or is associated with.
 */
public record HostOrganismDto(
        @ExportIgnore
        long id,
        @ExportField(name = "taxid", displayName = "Taxonomy ID", description = "NCBI Taxonomy identifier of the host organism")
        int taxid,
        @ExportField(name = "name", displayName = "Organism Name", description = "Scientific or common name of the host organism")
        String name
) implements ExportFormatSerializable {
    @Override
    public Map<String, Object> row() {
        return new HashMap<>() {{
            put("taxid", format(taxid));
            put("name", format(name));
        }};
    }

}
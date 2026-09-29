package com.bioinformatics.common.gene.dto;

import com.bioinformatics.shared.models.gene.ExportFormatSerializable;
import com.bioinformatics.shared.models.gene.export.ExportField;
import com.bioinformatics.shared.models.gene.export.ExportIgnore;
import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

/**
 * External database cross-reference record used in protein detail responses.
 * Links proteins to identifiers in other databases (e.g., UniProt, InterPro, PDB).
 */
@Builder
public record CrossReferenceDto(
        @ExportIgnore
        long id,
        @ExportField(name = "source", displayName = "Database Source", description = "External database name (e.g., EMBL, RefSeq, KEGG, Pfam, InterPro)")
        String source,
        @ExportField(name = "identifier", displayName = "Identifier", description = "Primary identifier in the external database")
        String identifier,
        @ExportField(name = "secondaryId", displayName = "Secondary ID", description = "Secondary identifier in the external database")
        String secondaryId,
        @ExportField(name = "tertiaryInfo", displayName = "Tertiary Info", description = "Additional information or metadata from the external database")
        String tertiaryInfo
) implements ExportFormatSerializable {
    @Override
    public Map<String, Object> row() {
        return new HashMap<>() {{
            put("source", format(source));
            put("identifier", format(identifier));
            put("secondaryId", format(secondaryId));
            put("tertiaryInfo", format(tertiaryInfo));
        }};
    }
}
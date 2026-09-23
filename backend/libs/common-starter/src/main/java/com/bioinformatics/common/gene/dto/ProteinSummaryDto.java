package com.bioinformatics.common.gene.dto;

import com.bioinformatics.shared.models.gene.ExportFormatSerializable;
import lombok.Builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight protein record used in paginated list responses.
 *
 * <p>Schema defined in documentation/api-contract.md — Shared Schemas — {@code ProteinSummary}.
 */
@Builder
public record ProteinSummaryDto(
        Long id,
        String accession,
        String entryName,
        String proteinFullName,
        String geneNamePrimary,
        String organismName,
        Integer taxid,
        Boolean reviewed,
        Integer length,
        Integer molecularWeight,
        Short evidenceLevel,
        List<String> keywords
) implements ExportFormatSerializable {

    @Override
    public Map<String, Object> row() {
        return new HashMap<>() {{
            put("Accession", format(accession));
            put("Entry Name", format(entryName));
            put("Protein Full Name", format(proteinFullName));
            put("Gene Name Primary", format(geneNamePrimary));
            put("Organism Name", format(organismName));
            put("Tax ID", format(taxid));
            put("Reviewed", reviewed);
            put("Length", format(length));
            put("Molecular Weight", format(molecularWeight));
            put("Evidence Level", format(EvidenceLevel.valueOf(evidenceLevel)));
            put("Keywords", keywords);
        }};
    }

    @Override
    public List<String> fieldsExcluded() {
        return List.of("id");
    }
}

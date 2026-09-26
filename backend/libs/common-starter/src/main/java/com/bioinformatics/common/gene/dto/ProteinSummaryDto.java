package com.bioinformatics.common.gene.dto;

import com.bioinformatics.shared.models.gene.ExportFormatSerializable;
import com.bioinformatics.shared.models.gene.export.ExportField;
import com.bioinformatics.shared.models.gene.export.ExportIgnore;
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
        @ExportIgnore
        Long id,
        @ExportField(name = "accession", displayName = "Accession", description = "Unique identifier for a protein")
        String accession,
        @ExportField(name = "entryName", displayName = "Entry Name", description = "Human-readable mnemonic identifier (format: GENENAME_ORGANISM)")
        String entryName,
        @ExportField(name = "proteinFullName", displayName = "Protein Full Name", description = "Full name of the protein")
        String proteinFullName,
        @ExportField(name = "geneNamePrimary", displayName = "Gene Name (Primary)", description = "Primary gene name or symbol")
        String geneNamePrimary,
        @ExportField(name = "organismName", displayName = "Organism Name", description = "Scientific name of the source organism")
        String organismName,
        @ExportField(name = "taxid", displayName = "Taxonomy ID", description = "NCBI Taxonomy identifier")
        Integer taxid,
        @ExportField(name = "reviewed", displayName = "Reviewed", description = "Whether the entry has been manually curated (true) or automatically annotated (false)")
        Boolean reviewed,
        @ExportField(name = "length", displayName = "Length", description = "Protein sequence length in amino acids")
        Integer length,
        @ExportField(name = "molecularWeight", displayName = "Molecular Weight", description = "Protein molecular weight in Daltons")
        Integer molecularWeight,
        @ExportField(name = "evidenceLevel", displayName = "Evidence Level", description = "Strength of experimental evidence (1=Protein, 2=Transcript, 3=Homology, 4=Predicted, 5=Uncertain)")
        Short evidenceLevel,
        @ExportField(name = "keywords", displayName = "Keywords", description = "Controlled vocabulary tags assigned to the protein")
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
}

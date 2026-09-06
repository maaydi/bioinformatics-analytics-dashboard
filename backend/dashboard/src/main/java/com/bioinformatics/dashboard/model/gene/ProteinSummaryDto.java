package com.bioinformatics.dashboard.model.gene;

import com.bioinformatics.dashboard.csv.CsvSerializable;
import lombok.Builder;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
) implements CsvSerializable {
    @Override
    public String row() {
        return Stream.of(
                format(id),
                format(accession),
                format(entryName),
                format(proteinFullName),
                format(geneNamePrimary),
                format(organismName),
                format(taxid),
                format(reviewed),
                format(length),
                format(molecularWeight),
                format(evidenceLevel),
                joinList(keywords)
        ).collect(Collectors.joining(separator()));
    }
}

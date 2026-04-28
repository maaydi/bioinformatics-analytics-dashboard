package com.bioinformatics.dashboard.gene.dto;

import java.util.List;

/**
 * Lightweight protein record used in paginated list responses.
 *
 * <p>Schema defined in documentation/api-contract.md — Shared Schemas — {@code ProteinSummary}.
 */
public record ProteinSummaryDto(
        Long         id,
        String       accession,
        String       entryName,
        String       proteinFullName,
        String       geneNamePrimary,
        String       organismName,
        Integer      taxid,
        Boolean      reviewed,
        Integer      length,
        Integer      molecularWeight,
        Short        evidenceLevel,
        List<String> keywords
) {}

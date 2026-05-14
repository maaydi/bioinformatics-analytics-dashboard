package com.bioinformatics.dashboard.gene.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ProteinDetailDto(

        Long id,

        String accession,
        String entryName,
        Boolean reviewed,

        LocalDate integratedDate,
        LocalDate sequenceDate,
        LocalDate updatedDate,
        Short sequenceVersion,
        Short entryVersion,

        String proteinFullName,
        String proteinShortName,
        String proteinEcNumber,

        String geneNamePrimary,
        String[] geneNameSynonyms,
        String[] geneOrfNames,
        String[] geneOrderedLocus,

        String organismName,
        String organismCommonName,
        Integer taxid,
        String[] lineage,

        Integer length,
        Integer molecularWeight,
        String sequenceChecksum,
        String sequence,

        Short evidenceLevel,

        String metadataJsonb,

        Instant createdAt,
        Instant updatedAt,

        List<String> keywords,
        List<ProteinFeatureDto> features,
        List<GoTermDto> goTerms,
        List<CrossReferenceDto> crossReferences

) {
}
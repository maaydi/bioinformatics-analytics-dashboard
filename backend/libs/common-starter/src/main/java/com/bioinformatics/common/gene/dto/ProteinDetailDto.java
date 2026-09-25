package com.bioinformatics.common.gene.dto;

import com.bioinformatics.shared.models.gene.ExportFormatSerializable;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Full protein detail record returned by {@code GET /api/genes/{id}}.
 * Contains all protein metadata, sequences, related features, GO terms, cross-references, and publications.
 *
 * <p>Schema defined in documentation/api-contract.md — Shared Schemas — {@code ProteinDetail}.
 */
@Builder
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
        Set<ProteinFeatureDto> features,
        Set<GoTermDto> goTerms,
        Set<CrossReferenceDto> crossReferences,
        Set<HostOrganismDto> hostOrganisms,
        Set<ProteinCommentDto> comments,
        Set<ProteinPublicationDto> publications

) implements ExportFormatSerializable {
    @Override
    public List<String> fieldsExcluded() {
        return List.of("id");
    }

    @Override
    public Map<String, Object> row() {
        return new HashMap<>() {{
            put("accession", format(accession));
            put("entryName", format(entryName));
            put("reviewed", reviewed);
            put("integratedDate", format(integratedDate));
            put("sequenceDate", format(sequenceDate));
            put("updatedDate", format(updatedDate));
            put("sequenceVersion", format(sequenceVersion));
            put("entryVersion", format(entryVersion));
            put("proteinFullName", format(proteinFullName));
            put("proteinShortName", format(proteinShortName));
            put("proteinEcNumber", format(proteinEcNumber));
            put("geneNamePrimary", format(geneNamePrimary));
            put("geneNameSynonyms", format(joinArray(geneNameSynonyms)));
            put("geneOrfNames", format(joinArray(geneOrfNames)));
            put("geneOrderedLocus", format(joinArray(geneOrderedLocus)));
            put("organismName", format(organismName));
            put("organismCommonName", format(organismCommonName));
            put("taxid", format(taxid));
            put("lineage", format(joinArray(lineage)));
            put("length", format(length));
            put("molecularWeight", format(molecularWeight));
            put("sequenceChecksum", format(sequenceChecksum));
            put("sequence", format(sequence));
            put("evidenceLevel", format(EvidenceLevel.valueOf(evidenceLevel)));
            put("metadataJsonb", format(metadataJsonb));
            put("createdAt", format(createdAt));
            put("updatedAt", format(updatedAt));
            put("keywords", format(joinList(keywords)));
            put("features", formatFeatures(features));
            put("goTerms", formatGoTerms(goTerms));
            put("crossReferences", formatCrossReferences(crossReferences));
            put("hostOrganisms", formatHostOrganisms(hostOrganisms));
            put("comments", formatComment(comments));
            put("publications", formatPublications(publications));

        }};
    }


    private String formatFeatures(Set<ProteinFeatureDto> features) {
        var result = Objects.requireNonNullElse(features, new HashSet<ProteinFeatureDto>())
                .stream()
                .map(ProteinFeatureDto::featureType)
                .toList();
        return format(joinList(result));

    }

    private String formatGoTerms(Set<GoTermDto> goTerms) {
        var result = Objects.requireNonNullElse(goTerms, new HashSet<GoTermDto>())
                .stream()
                .map(GoTermDto::goId)
                .toList();
        return format(joinList(result));
    }

    private String formatCrossReferences(Set<CrossReferenceDto> refs) {
        var result = Objects.requireNonNullElse(refs, new HashSet<CrossReferenceDto>())
                .stream().map(r -> r.source() + ":" + r.identifier())
                .toList();
        return format(joinList(result));
    }

    private String formatHostOrganisms(Set<HostOrganismDto> hosts) {
        var result = Objects.requireNonNullElse(hosts, new HashSet<HostOrganismDto>())
                .stream().map(HostOrganismDto::name)
                .toList();
        return format(joinList(result));
    }

    private String formatComment(Set<ProteinCommentDto> comments) {
        var result = Objects.requireNonNullElse(comments, new HashSet<ProteinCommentDto>())
                .stream().map(r -> r.commentType() + ": " + r.text())
                .toList();
        return format(joinList(result));
    }

    private String formatPublications(Set<ProteinPublicationDto> publications) {
        var result = Objects.requireNonNullElse(publications, new HashSet<ProteinPublicationDto>())
                .stream().map(ProteinPublicationDto::pubmedId)
                .toList();
        return format(joinList(result));
    }

}
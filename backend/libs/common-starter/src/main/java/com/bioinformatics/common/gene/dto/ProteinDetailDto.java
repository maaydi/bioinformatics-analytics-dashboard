package com.bioinformatics.common.gene.dto;

import com.bioinformatics.shared.models.gene.ExportFormatSerializable;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
            put("features", format(formatFeatures(features)));
            put("goTerms", format(formatGoTerms(goTerms)));
            put("crossReferences", format(formatCrossReferences(crossReferences)));
            put("hostOrganisms", format(formatHostOrganisms(hostOrganisms)));
            put("comments", format(formatComment(comments)));
            put("publications", format(formatPublications(publications)));

        }};
    }


    private String formatFeatures(Set<ProteinFeatureDto> features) {
        if (features == null || features.isEmpty()) {
            return "";
        }

        var result = features.stream()
                .map(f -> f.featureId() + ":" + f.featureType() + " - " + f.note())
                .collect(Collectors.joining(" | "));
        return format(result);
    }

    private String formatGoTerms(Set<GoTermDto> goTerms) {
        if (goTerms == null || goTerms.isEmpty()) {
            return "";
        }

        var result = goTerms.stream()
                .map(g -> g.id() + ":" + g.goId())
                .collect(Collectors.joining(" | "));
        return format(result);
    }

    private String formatCrossReferences(Set<CrossReferenceDto> refs) {
        if (refs == null || refs.isEmpty()) {
            return "";
        }

        var result = refs.stream()
                .map(r -> r.identifier() + ":" + r.source())
                .collect(Collectors.joining(" | "));
        return format(result);
    }

    private String formatHostOrganisms(Set<HostOrganismDto> hosts) {
        if (hosts == null || hosts.isEmpty()) {
            return "";
        }

        var result = hosts.stream()
                .map(r -> r.id() + ":" + r.name())
                .collect(Collectors.joining(" | "));
        return format(result);
    }

    private String formatComment(Set<ProteinCommentDto> comments) {
        if (comments == null || comments.isEmpty()) {
            return "";
        }

        var result = comments.stream()
                .map(r -> r.commentType() + ":" + r.text())
                .collect(Collectors.joining(" | "));
        return format(result);
    }

    private String formatPublications(Set<ProteinPublicationDto> publications) {
        if (publications == null || publications.isEmpty()) {
            return "";
        }

        var result = publications.stream()
                .map(r -> r.pubmedId() + "[" + r.refNumber() + "]: " + r.title() + " - " + r.authors())
                .collect(Collectors.joining(" | "));
        return format(result);
    }

}
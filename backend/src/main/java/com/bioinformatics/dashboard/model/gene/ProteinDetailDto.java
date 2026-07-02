package com.bioinformatics.dashboard.model.gene;

import com.bioinformatics.dashboard.csv.CsvSerializable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

) implements CsvSerializable {
    @Override
    public String row() {
        return Stream.of(

                        format(id()),

                        format(accession()),
                        format(entryName()),
                        format(reviewed()),

                        format(integratedDate()),
                        format(sequenceDate()),
                        format(updatedDate()),

                        format(sequenceVersion()),
                        format(entryVersion()),

                        format(proteinFullName()),
                        format(proteinShortName()),
                        format(proteinEcNumber()),

                        format(geneNamePrimary()),
                        format(joinArray(geneNameSynonyms())),
                        format(joinArray(geneOrfNames())),
                        format(joinArray(geneOrderedLocus())),

                        format(organismName()),
                        format(organismCommonName()),
                        format(taxid()),

                        format(joinArray(lineage())),

                        format(length()),
                        format(molecularWeight()),

                        format(sequenceChecksum()),
                        format(sequence()),

                        format(evidenceLevel()),

                        format(metadataJsonb()),

                        format(createdAt()),
                        format(updatedAt()),

                        format(joinList(keywords())),
                        format(formatFeatures(features())),
                        format(formatGoTerms(goTerms())),
                        format(formatCrossReferences(crossReferences())),
                        format(formatHostOrganisms(hostOrganisms())),
                        format(formatComment(comments())),
                        format(formatPublications(publications()))

                )
                .collect(Collectors.joining(separator()));
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
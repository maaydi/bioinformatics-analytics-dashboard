package com.bioinformatics.common.gene.dto;

import com.bioinformatics.shared.models.gene.ExportFormatSerializable;
import com.bioinformatics.shared.models.gene.export.ExportField;
import com.bioinformatics.shared.models.gene.export.ExportIgnore;
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

        @ExportIgnore
        Long id,
        @ExportField(name = "accession", displayName = "Accession", description = "Unique identifier for a protein")
        String accession,
        @ExportField(name = "entryName", displayName = "Entry Name", description = "Human-readable mnemonic identifier (format: GENENAME_ORGANISM)")
        String entryName,
        @ExportField(name = "reviewed", displayName = "Reviewed", description = "Whether the entry has been manually curated (true) or automatically annotated (false)")
        Boolean reviewed,

        @ExportField(name = "integratedDate", displayName = "Integrated Date", description = "Date when the entry was integrated into UniProtKB")
        LocalDate integratedDate,
        @ExportField(name = "sequenceDate", displayName = "Sequence Date", description = "Date of the last sequence update")
        LocalDate sequenceDate,
        @ExportField(name = "updatedDate", displayName = "Updated Date", description = "Date of the last entry update")
        LocalDate updatedDate,
        @ExportField(name = "sequenceVersion", displayName = "Sequence Version", description = "Version number of the sequence")
        Short sequenceVersion,
        @ExportField(name = "entryVersion", displayName = "Entry Version", description = "Version number of the entry")
        Short entryVersion,

        @ExportField(name = "proteinFullName", displayName = "Protein Full Name", description = "Full name of the protein")
        String proteinFullName,
        @ExportField(name = "proteinShortName", displayName = "Protein Short Name", description = "Short or alternative name of the protein")
        String proteinShortName,
        @ExportField(name = "proteinEcNumber", displayName = "Protein EC Number", description = "Enzyme Commission classification number")
        String proteinEcNumber,

        @ExportField(name = "geneNamePrimary", displayName = "Gene Name (Primary)", description = "Primary gene name or symbol")
        String geneNamePrimary,
        @ExportField(name = "geneNameSynonyms", displayName = "Gene Name (Synonyms)", description = "Alternative gene names or synonyms")
        String[] geneNameSynonyms,
        @ExportField(name = "geneOrfNames", displayName = "Gene ORF Names", description = "Open Reading Frame (ORF) names")
        String[] geneOrfNames,
        @ExportField(name = "geneOrderedLocus", displayName = "Gene Ordered Locus", description = "Ordered locus names")
        String[] geneOrderedLocus,

        @ExportField(name = "organismName", displayName = "Organism Name", description = "Scientific name of the source organism")
        String organismName,
        @ExportField(name = "organismCommonName", displayName = "Organism Common Name", description = "Common or vernacular name of the organism")
        String organismCommonName,
        @ExportField(name = "taxid", displayName = "Taxonomy ID", description = "NCBI Taxonomy identifier")
        Integer taxid,
        @ExportField(name = "lineage", displayName = "Taxonomic Lineage", description = "Taxonomic hierarchy of the organism")
        String[] lineage,

        @ExportField(name = "length", displayName = "Length", description = "Protein sequence length in amino acids")
        Integer length,
        @ExportField(name = "molecularWeight", displayName = "Molecular Weight", description = "Protein molecular weight in Daltons")
        Integer molecularWeight,
        @ExportField(name = "sequenceChecksum", displayName = "Sequence Checksum", description = "CRC64 checksum for sequence integrity verification")
        String sequenceChecksum,
        @ExportField(name = "sequence", displayName = "Sequence", description = "Amino acid sequence of the protein")
        String sequence,

        @ExportField(name = "evidenceLevel", displayName = "Evidence Level", description = "Strength of experimental evidence (1=Protein, 2=Transcript, 3=Homology, 4=Predicted, 5=Uncertain)")
        Short evidenceLevel,

        @ExportField(name = "metadataJsonb", displayName = "Meta data", description = "Protein meta data as json format")
        String metadataJsonb,

        @ExportIgnore
        Instant createdAt,
        @ExportIgnore
        Instant updatedAt,

        @ExportField(name = "keywords", displayName = "Keywords", description = "Controlled vocabulary tags assigned to the protein")
        List<String> keywords,
        @ExportField(name = "features", displayName = "Feature Type", description = "Type of annotated feature (e.g., CHAIN, DOMAIN, SIGNAL, BINDING)")
        Set<ProteinFeatureDto> features,
        @ExportField(name = "goTerms", displayName = "GO ID", description = "Gene Ontology term identifier (e.g., GO:0046782)")
        Set<GoTermDto> goTerms,
        @ExportField(name = "crossReferences", displayName = "Cross References", description = "External database name (e.g., EMBL, RefSeq, KEGG, Pfam, InterPro) & Primary identifier in the external database")
        Set<CrossReferenceDto> crossReferences,
        @ExportField(name = "hostOrganisms", displayName = "Host Organism Name", description = "Scientific or common name of the host organism")
        Set<HostOrganismDto> hostOrganisms,
        @ExportField(name = "comments", displayName = "Comment Type & Text", description = "Category of comment (e.g., FUNCTION, CATALYTIC_ACTIVITY, SUBCELLULAR_LOCATION, DISEASE) & Annotated comment text with evidence codes and cross-references")
        Set<ProteinCommentDto> comments,
        @ExportField(name = "publications", displayName = "Publication PubMed ID", description = "PubMed unique identifier for the publication")
        Set<ProteinPublicationDto> publications

) implements ExportFormatSerializable {

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
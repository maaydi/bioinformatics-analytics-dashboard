package com.bioinformatics.dashboard.model.gene;

/**
 * Publication record citing evidence for protein annotations and data.
 * Contains PubMed ID and bibliographic details for scientific literature references.
 */
public record ProteinPublicationDto(

        Long id,

        Short refNumber,

        String pubmedId,

        String doi,

        String authors,

        String title,

        String journal

) {
}

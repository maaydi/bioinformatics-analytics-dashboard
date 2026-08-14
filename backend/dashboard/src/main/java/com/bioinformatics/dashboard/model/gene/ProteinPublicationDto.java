package com.bioinformatics.dashboard.model.gene;

import lombok.Builder;

/**
 * Publication record citing evidence for protein annotations and data.
 * Contains PubMed ID and bibliographic details for scientific literature references.
 */
@Builder
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

package com.bioinformatics.dashboard.model.gene;

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

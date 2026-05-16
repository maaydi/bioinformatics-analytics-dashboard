package com.bioinformatics.dashboard.gene.dto;

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

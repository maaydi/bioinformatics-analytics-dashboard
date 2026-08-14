package com.bioinformatics.dashboard.model.uniprot.dto;

public record Sequence(
        String value,
        int length,
        int molWeight,
        String crc64,
        String md5
) {
}

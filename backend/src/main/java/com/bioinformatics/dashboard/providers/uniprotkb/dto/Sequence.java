package com.bioinformatics.dashboard.providers.uniprotkb.dto;

public record Sequence(
        String value,
        int length,
        int molWeight,
        String crc64,
        String md5
) {
}

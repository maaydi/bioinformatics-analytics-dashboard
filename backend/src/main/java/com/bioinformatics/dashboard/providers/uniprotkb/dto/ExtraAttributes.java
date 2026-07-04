package com.bioinformatics.dashboard.providers.uniprotkb.dto;

public record ExtraAttributes(
        CountByCommentType countByCommentType,
        CountByFeatureType countByFeatureType,
        String uniParcId
) {
}

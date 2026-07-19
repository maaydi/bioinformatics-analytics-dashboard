package com.bioinformatics.dashboard.model.uniprot.dto;

public record ExtraAttributes(
        CountByCommentType countByCommentType,
        CountByFeatureType countByFeatureType,
        String uniParcId
) {
}

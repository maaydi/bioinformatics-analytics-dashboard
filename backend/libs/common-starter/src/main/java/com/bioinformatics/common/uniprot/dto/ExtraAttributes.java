package com.bioinformatics.common.uniprot.dto;

public record ExtraAttributes(
        CountByCommentType countByCommentType,
        CountByFeatureType countByFeatureType,
        String uniParcId
) {
}

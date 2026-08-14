package com.bioinformatics.dashboard.model.uniprot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CountByFeatureType(
        Integer Chain,
        Integer Transmembrane,
        Integer Glycosylation,
        @JsonProperty("Natural variant") Integer naturalVariant
) {
}

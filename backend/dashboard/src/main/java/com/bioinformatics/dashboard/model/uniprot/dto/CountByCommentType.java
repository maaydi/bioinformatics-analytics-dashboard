package com.bioinformatics.dashboard.model.uniprot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CountByCommentType(
        Integer FUNCTION,
        Integer INTERACTION,
        @JsonProperty("SUBCELLULAR LOCATION") Integer subcellularLocation,
        Integer DISEASE,
        Integer SIMILARITY
) {
}

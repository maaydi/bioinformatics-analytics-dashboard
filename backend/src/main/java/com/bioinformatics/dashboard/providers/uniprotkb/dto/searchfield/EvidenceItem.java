package com.bioinformatics.dashboard.providers.uniprotkb.dto.searchfield;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EvidenceItem(
        String name,
        String code
) {
}

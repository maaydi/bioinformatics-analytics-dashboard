package com.bioinformatics.dashboard.providers.uniprotkb.dto.searchfield;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EvidenceGroup(
        @JsonProperty("groupName") String groupName,
        List<EvidenceItem> items
) {
}

package com.bioinformatics.dashboard.providers.uniprotkb.dto.searchfield;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchField(
        String id,
        String label,
        @JsonProperty("itemType") String itemType,
        String term,
        @JsonProperty("dataType") String dataType,
        @JsonProperty("fieldType") String fieldType,
        String example,
        String regex,
        String autoComplete,
        @JsonProperty("autoCompleteQueryTerm") String autoCompleteQueryTerm,
        @JsonProperty("valuePrefix") String valuePrefix,
        List<String> tags,
        List<FieldValue> values,
        List<EvidenceGroup> evidenceGroups,
        List<SearchField> items,
        List<SearchField> siblings
) {
}


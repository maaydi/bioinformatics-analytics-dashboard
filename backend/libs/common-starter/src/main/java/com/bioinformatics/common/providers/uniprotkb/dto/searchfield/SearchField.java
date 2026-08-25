package com.bioinformatics.common.providers.uniprotkb.dto.searchfield;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A search field definition from the UniProt KB search configuration.
 *
 * <p>Represents a queryable field in the UniProt knowledge base, including its metadata,
 * allowed values, evidence classifications, and hierarchical relationships to other fields.</p>
 *
 * <p>Fields may be organized hierarchically (via {@code items} and {@code siblings}),
 * enabling faceted search UI construction and query validation. Only fields with
 * {@code itemType} of "sibling_group" populate the {@code siblings} list; others use {@code items}
 * to represent child fields.</p>
 *
 * @param id                    the unique identifier for this field
 * @param label                 human-readable display name for the field
 * @param itemType              the field's role in the hierarchy (e.g., "group", "sibling_group")
 * @param term                  the query term to use in UniProt search syntax (e.g., "accession")
 * @param dataType              expected data type for values (e.g., "STRING", "INTEGER")
 * @param fieldType             classification of the field's purpose (e.g., "general", "metadata")
 * @param example               example value to demonstrate field usage
 * @param regex                 optional regex pattern to validate user input against this field
 * @param autoComplete          whether the API supports autocomplete for this field
 * @param autoCompleteQueryTerm the query term to use when requesting autocompletion suggestions
 * @param valuePrefix           optional string prefix to prepend to user values in queries
 * @param tags                  searchable labels or keywords for categorizing this field
 * @param values                enumerated allowed values, if the field is restricted to a finite set
 * @param evidenceGroups        groupings of evidence types applicable to this field
 * @param items                 child field definitions (used when {@code itemType} is not "sibling_group")
 * @param siblings              peer field definitions at the same level (used when {@code itemType} is "sibling_group")
 */
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


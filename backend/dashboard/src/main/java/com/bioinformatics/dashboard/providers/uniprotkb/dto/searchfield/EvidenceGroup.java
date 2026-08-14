package com.bioinformatics.dashboard.providers.uniprotkb.dto.searchfield;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A categorized grouping of evidence types for a search field.
 *
 * <p>Organizes related evidence types (e.g., all evidence types for "protein existence")
 * under a descriptive category name, enabling users to apply evidence filters
 * when querying the UniProt knowledge base.</p>
 *
 * @param groupName the display name for this evidence category
 * @param items     the list of evidence types in this group
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EvidenceGroup(
        @JsonProperty("groupName") String groupName,
        List<EvidenceItem> items
) {
}

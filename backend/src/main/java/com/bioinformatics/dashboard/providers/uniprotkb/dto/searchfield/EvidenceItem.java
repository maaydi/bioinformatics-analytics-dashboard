package com.bioinformatics.dashboard.providers.uniprotkb.dto.searchfield;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A single evidence type within an evidence group.
 *
 * <p>Represents a category of evidence (e.g., "computed", "observed", "inferred")
 * that can be used to filter or qualify search results within a specific search field.</p>
 *
 * @param name the display name of the evidence type
 * @param code the machine-readable code for this evidence type, used in query construction
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EvidenceItem(
        String name,
        String code
) {
}

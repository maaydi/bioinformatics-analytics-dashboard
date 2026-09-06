package com.bioinformatics.common.providers.uniprotkb.dto.searchfield;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * An allowed value for a restricted (enumerated) search field.
 *
 * <p>Represents one of the finite set of values that may be assigned to a search field
 * when the field's domain is restricted (e.g., organism types, database names).</p>
 *
 * @param name  a human-readable label for this value
 * @param value the actual value to use in UniProt search syntax
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldValue(
        String name,
        String value
) {
}

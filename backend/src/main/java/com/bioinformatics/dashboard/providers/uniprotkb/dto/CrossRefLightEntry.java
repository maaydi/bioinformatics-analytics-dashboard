package com.bioinformatics.dashboard.providers.uniprotkb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A lightweight entry from the UniProt database cross-reference search.
 *
 * <p>Represents a single database name or abbreviation matched by the database search API,
 * useful for identifying related bioinformatics databases and cross-link targets.</p>
 *
 * @param abbrev the database abbreviation or short name (e.g., "PDB", "UNIGENE")
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CrossRefLightEntry(String abbrev) {
}

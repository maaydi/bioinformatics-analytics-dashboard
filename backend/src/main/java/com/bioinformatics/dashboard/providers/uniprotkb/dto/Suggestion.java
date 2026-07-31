package com.bioinformatics.dashboard.providers.uniprotkb.dto;

/**
 * A single search term suggestion provided by UniProt autocomplete.
 *
 * <p>Represents a candidate query term that a user might type when searching,
 * useful for enabling type-ahead search interfaces and reducing user input errors.</p>
 *
 * @param value the suggested search term as a human-readable string
 * @param id    the unique identifier for this suggestion (e.g., protein accession, gene name)
 */
public record Suggestion(String value, String id) {
}

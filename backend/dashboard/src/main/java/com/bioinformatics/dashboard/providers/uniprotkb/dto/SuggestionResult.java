package com.bioinformatics.dashboard.providers.uniprotkb.dto;

import java.util.List;

/**
 * A collection of search term suggestions from the UniProt autocomplete API.
 *
 * <p>Wraps the results of a single suggestion query, including the original query,
 * the dictionary/field against which suggestions were computed, and the matching suggestions.</p>
 *
 * @param query       the original search term or prefix provided to the autocomplete endpoint
 * @param dictionary  the UniProt field or dictionary code (e.g., "accession", "gene_name")
 * @param suggestions the list of matching suggestions for the query
 */
public record SuggestionResult(String query, String dictionary, List<Suggestion> suggestions) {
}

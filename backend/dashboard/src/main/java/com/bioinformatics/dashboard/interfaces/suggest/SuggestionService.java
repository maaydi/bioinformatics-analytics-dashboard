package com.bioinformatics.dashboard.interfaces.suggest;

import com.bioinformatics.common.providers.Provider;

import java.util.List;

/**
 * Service contract for field-level autocomplete suggestions during gene search composition.
 *
 * <p>Implementations resolve a provider-specific suggestion source such as PostgreSQL or the UniProt API and expose a
 * consistent search experience regardless of the underlying backend.</p>
 */
public interface SuggestionService extends Provider {
    /**
     * Returns the target field this suggestion implementation supports.
     *
     * @return suggestion field key
     */
    String field();

    /**
     * Retrieves suggestions matching the query for this implementation's field.
     *
     * @param query the search query
     * @return list of up to 10 matching suggestions
     */
    List<String> suggest(String query);

    /**
     * Retrieves suggestions for a specific field matching the query.
     * Default implementation delegates to {@link #suggest(String)}.
     *
     * @param field the target field
     * @param query the search query
     * @return list of up to 10 matching suggestions
     */
    default List<String> suggest(String field, String query) {
        return suggest(query);
    }
}

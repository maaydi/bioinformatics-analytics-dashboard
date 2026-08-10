package com.bioinformatics.dashboard.interfaces.suggest;

import com.bioinformatics.dashboard.interfaces.Provider;

import java.util.List;

/**
 * Service interface for retrieving field suggestions based on query.
 */
public interface SuggestionService extends Provider {
    /**
     * @return the target field for suggestions
     */
    String field();

    /**
     * Retrieves suggestions matching the query.
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

package com.bioinformatics.dashboard.interfaces.suggest;

import com.bioinformatics.dashboard.interfaces.Provider;

import java.util.List;

/**
 * Suggestion service interface : Retrieve list of suggestion for a given field based on a query
 *
 */
public interface SuggestionService extends Provider {
    /**
     * Concerned field for suggestion
     */
    String field();

    /**
     * Retrieves a list of suggestions based on the provided query for one of the given fields.
     *
     * @param query The input query string for which suggestions are to be retrieved.
     * @return A list of suggestion strings that match the input query.
     *
     */
    List<String> suggest(String query);


    /**
     * Retrieves a list of suggestions based on the provided field and query.
     *
     * @param field The field for which suggestions are to be retrieved.
     * @param query The input query string for which suggestions are to be retrieved.
     * @return A list of 10 suggestion strings that match the input query.
     *
     */
    default List<String> suggest(String field, String query) {
        return suggest(query);
    }
}

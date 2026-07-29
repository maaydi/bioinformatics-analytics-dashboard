package com.bioinformatics.dashboard.gene.suggest;

import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for autocomplete suggestions.
 * Provides suggestions for protein search fields.
 */
@RestController
@RequestMapping("/api/autocomplete")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class AutoCompleteController {
    private final SuggestionService suggestionService;

    /**
     * Retrieves suggestions for a given field and query.
     *
     * @param field the search field (e.g., Accession, EntryName, FeatureType)
     * @param query the search query
     * @return list of up to 10 matching suggestions
     */
    @GetMapping
    public List<String> suggest(@RequestParam(name = "field") String field, @RequestParam(name = "query") String query) {
        return suggestionService.suggest(field, query);
    }
}

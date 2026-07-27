package com.bioinformatics.dashboard.gene.autocomplete;

import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/autocomplete")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class AutoCompleteController {
    private final SuggestionService suggestionService;

    /**
     * Returns a list of suggestions based on the provided field and query.
     *
     * @param field The field to search for suggestions (e.g., "Accession", "EntryName", "FeatureType", "GoTermId").
     * @param query The query string to search for suggestions.
     * @return A list of 10 suggestions matching the query for the specified field.
     *
     */
    @GetMapping
    public List<String> suggest(@RequestParam(name = "field") String field, @RequestParam(name = "query") String query) {
        return suggestionService.suggest(field, query);
    }
}

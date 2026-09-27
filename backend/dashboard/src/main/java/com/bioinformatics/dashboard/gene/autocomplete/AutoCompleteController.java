package com.bioinformatics.dashboard.gene.autocomplete;

import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing autocomplete suggestions for gene-search fields.
 *
 * <p>This endpoint allows the UI to request candidate values for specific protein metadata fields while the user types,
 * reducing the burden of manual query construction and improving the search experience.</p>
 */
@RestController
@RequestMapping("/api/autocomplete")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
@Slf4j
public class AutoCompleteController {
    private final SuggestionService suggestionService;

    /**
     * Retrieves suggestions for a given field and query.
     *
     * @param field the target field name (for example accession, geneNamePrimary, featureType)
     * @param query the current user input used to match suggestions
     * @return a list of up to 10 matching suggestions
     */
    @GetMapping
    public List<String> suggest(@RequestParam(name = "field") String field, @RequestParam(name = "query") String query) {
        log.info("[DASHBOARD][AUTOCOMPLETE] field={} query={}", field, query);
        return suggestionService.suggest(field, query);
    }
}
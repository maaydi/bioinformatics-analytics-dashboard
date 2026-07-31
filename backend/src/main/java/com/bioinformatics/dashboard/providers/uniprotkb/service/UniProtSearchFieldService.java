package com.bioinformatics.dashboard.providers.uniprotkb.service;

import com.bioinformatics.dashboard.providers.uniprotkb.dto.searchfield.SearchField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Extraction and caching service for UniProt feature types.
 *
 * <p>Loads the UniProt search field configuration once and extracts a hierarchical map
 * of feature types (prefixed with "ft_") organized by their parent category labels.
 * Results are cached for 30 days and automatically refreshed thereafter.</p>
 *
 * <p>This service is used primarily for API clients that need to discover and validate
 * feature type constraints without repeated network calls.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UniProtSearchFieldService {

    private final SearchFieldRestService restService;

    private Map<String, List<String>> cachedFeatureTypes;
    private Instant lastUpdate = Instant.now();

    /**
     * Recursively traverses a search field hierarchy to extract feature type identifiers
     * that belong under a given parent label.
     *
     * <p>This is an internal helper for extracting the hierarchical feature type map
     * from the nested SearchField structure provided by UniProt's configuration API.</p>
     *
     * @param field       the current field node to process
     * @param parentLabel the label of the parent category (used as the map key)
     * @param result      the accumulator map to populate with feature types
     */
    private static void traverse(SearchField field, String parentLabel,
                                 Map<String, List<String>> result) {
        if (!field.itemType().equalsIgnoreCase("sibling_group")
                && field.items() != null
                && !field.items().isEmpty()) {
            for (var child : field.items()) {
                traverse(child, child.label(), result);
            }
        }
        if (field.itemType().equalsIgnoreCase("sibling_group")
                && field.siblings() != null
                && !field.siblings().isEmpty()) {
            var ftIds = field.siblings().stream()
                    .filter(s -> s.term() != null && s.term().startsWith("ft_"))
                    .map(SearchField::term)
                    .toList();
            result.computeIfAbsent(parentLabel, _ -> new ArrayList<>()).addAll(ftIds);
        }
    }

    /**
     * Retrieves the cached feature type mapping, refreshing from the API if the cache
     * has expired (older than 30 days).
     *
     * <p>This method implements a simple time-based cache eviction strategy to balance
     * freshness with API call reduction.</p>
     *
     * @return an immutable map where keys are parent category labels and values are
     * lists of feature type identifiers (e.g., ["ft_act_site", "ft_binding", ...])
     */
    public Map<String, List<String>> getCachedFeatureTypes() {
        var now = Instant.now();
        if (cachedFeatureTypes == null || now.isAfter(lastUpdate.plusSeconds(30 * 24 * 3600))) {
            var response = restService.loadSearchFieldConfig();
            if (response.hasBody() && response.getBody() != null) {
                var fields = extractFeatureTypes(response.getBody());
                cachedFeatureTypes = Collections.unmodifiableMap(fields);
                lastUpdate = Instant.now();
                log.info("Update Cached Feature type : Found {} FT", cachedFeatureTypes.size());
            }
        }
        return cachedFeatureTypes;
    }

    /**
     * Extracts the hierarchical feature type mapping from the raw search field list.
     *
     * <p>Traverses the field tree to collect all feature type identifiers,
     * organizing them under their parent category labels.</p>
     *
     * @param fields the top-level search field definitions from UniProt
     * @return a map of category labels to lists of feature type identifiers
     */
    private Map<String, List<String>> extractFeatureTypes(List<SearchField> fields) {
        var result = new LinkedHashMap<String, List<String>>();
        for (SearchField field : fields) {
            traverse(field, null, result);
        }
        return result;
    }
}

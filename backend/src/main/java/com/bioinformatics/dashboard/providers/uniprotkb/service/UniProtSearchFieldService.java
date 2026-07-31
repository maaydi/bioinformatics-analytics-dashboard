package com.bioinformatics.dashboard.providers.uniprotkb.service;

import com.bioinformatics.dashboard.providers.uniprotkb.dto.searchfield.SearchField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class UniProtSearchFieldService {

    private final SearchFieldRestService restService;

    private Map<String, List<String>> cachedFeatureTypes;
    private Instant lastUpdate = Instant.now();

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

    private Map<String, List<String>> extractFeatureTypes(List<SearchField> fields) {
        var result = new LinkedHashMap<String, List<String>>();
        for (SearchField field : fields) {
            traverse(field, null, result);
        }
        return result;
    }
}

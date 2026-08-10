package com.bioinformatics.dashboard.providers.uniprotkb.suggest;

import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import com.bioinformatics.dashboard.providers.uniprotkb.AbstractUniprotKbProvider;
import com.bioinformatics.dashboard.providers.uniprotkb.service.UniProtSearchFieldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * uniprot API suggestion provider for protein feature types.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeatureTypeUniprotApiSuggestion extends AbstractUniprotKbProvider implements SuggestionService {
    private final UniProtSearchFieldService service;

    @Override
    public String field() {
        return "FeatureType";
    }

    @Override
    public List<String> suggest(String query) {
        var ft = service.getCachedFeatureTypes();
        return ft.keySet().stream()
                .filter(Objects::nonNull)
                .filter(item -> item.toLowerCase().contains(query.toLowerCase()))
                .limit(10)
                .toList();
    }


}

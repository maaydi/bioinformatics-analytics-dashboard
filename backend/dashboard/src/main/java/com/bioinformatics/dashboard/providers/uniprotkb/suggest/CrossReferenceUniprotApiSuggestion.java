package com.bioinformatics.dashboard.providers.uniprotkb.suggest;

import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import com.bioinformatics.dashboard.providers.uniprotkb.AbstractUniprotKbProvider;
import com.bioinformatics.dashboard.providers.uniprotkb.dto.CrossRefLightEntry;
import com.bioinformatics.dashboard.providers.uniprotkb.service.DatabaseRestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * uniprot API suggestion provider for cross-reference sources.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CrossReferenceUniprotApiSuggestion extends AbstractUniprotKbProvider implements SuggestionService {
    private final DatabaseRestService databaseRestService;

    @Override
    public String field() {
        return "CrossReferenceSource";
    }

    @Override
    public List<String> suggest(String query) {
        try {
            var result = databaseRestService.searchAll("((name:%s*))".formatted(query), 50);
            if (result.hasBody() && result.getBody() != null) {
                return result.getBody().results().stream()
                        .map(CrossRefLightEntry::abbrev)
                        .distinct()
                        .limit(10)
                        .toList();
            }

        } catch (Exception e) {
            log.warn("Error while searching for CrossReferenceSource with query {} : {}", query, e.getMessage());
        }
        return new ArrayList<>();
    }

}

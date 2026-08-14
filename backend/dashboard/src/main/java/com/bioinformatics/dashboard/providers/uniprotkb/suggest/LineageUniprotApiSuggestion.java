package com.bioinformatics.dashboard.providers.uniprotkb.suggest;

import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import com.bioinformatics.dashboard.providers.uniprotkb.AbstractUniprotKbProvider;
import com.bioinformatics.dashboard.providers.uniprotkb.dto.Suggestion;
import com.bioinformatics.dashboard.providers.uniprotkb.service.SuggesterRestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * uniprot API suggestion provider for organism lineage.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LineageUniprotApiSuggestion extends AbstractUniprotKbProvider implements SuggestionService {
    private final SuggesterRestService suggesterRestService;

    @Override
    public String field() {
        return "Lineage";
    }

    public List<String> suggest(String query) {
        try {
            var result = suggesterRestService.searchAll("taxonomy", query);
            if (result.hasBody() && result.getBody() != null) {
                return result.getBody().suggestions().stream()
                        .map(Suggestion::value)
                        .distinct()
                        .limit(10)
                        .toList();
            }

        } catch (Exception e) {
            log.warn("Error while searching for Taxonomy Lineage with query {} : {}", query, e.getMessage());
        }
        return new ArrayList<>();
    }


}

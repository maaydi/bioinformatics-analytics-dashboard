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
 * uniprot API suggestion provider for GO term identifiers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoTermIdUniprotApiSuggestion extends AbstractUniprotKbProvider implements SuggestionService {
    private final SuggesterRestService suggesterRestService;


    @Override
    public String field() {
        return "GoTermId";
    }

    @Override
    public List<String> suggest(String query) {
        try {
            var result = suggesterRestService.searchAll("go", query);
            if (result.hasBody() && result.getBody() != null) {
                return result.getBody().suggestions().stream()
                        .map(Suggestion::id)
                        .map("GO:"::concat)
                        .distinct()
                        .limit(10)
                        .toList();
            }

        } catch (Exception e) {
            log.warn("Error while searching for Go Term with query {} : {}", query, e.getMessage());
        }
        return new ArrayList<>();
    }


}

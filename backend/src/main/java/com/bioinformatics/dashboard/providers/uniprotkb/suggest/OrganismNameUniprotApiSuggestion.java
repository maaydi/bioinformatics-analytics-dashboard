package com.bioinformatics.dashboard.providers.uniprotkb.suggest;

import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import com.bioinformatics.dashboard.providers.uniprotkb.AbstractUniprotKbProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * uniprot API suggestion provider for organism names.
 */
@Component
@RequiredArgsConstructor
public class OrganismNameUniprotApiSuggestion extends AbstractUniprotKbProvider implements SuggestionService {


    @Override
    public String field() {
        return "OrganismName";
    }

    @Override
    public List<String> suggest(String query) {
        throw new UnsupportedOperationException("KeywordName suggestion is not supported yet.");
    }


}

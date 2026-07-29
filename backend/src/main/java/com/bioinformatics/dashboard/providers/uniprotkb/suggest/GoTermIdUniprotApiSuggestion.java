package com.bioinformatics.dashboard.providers.uniprotkb.suggest;

import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import com.bioinformatics.dashboard.providers.uniprotkb.AbstractUniprotKbProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * uniprot API suggestion provider for GO term identifiers.
 */
@Component
@RequiredArgsConstructor
public class GoTermIdUniprotApiSuggestion extends AbstractUniprotKbProvider implements SuggestionService {


    @Override
    public String field() {
        return "GoTermId";
    }

    @Override
    public List<String> suggest(String query) {
        throw new UnsupportedOperationException("GO term identifier suggestion is not supported yet.");
    }


}

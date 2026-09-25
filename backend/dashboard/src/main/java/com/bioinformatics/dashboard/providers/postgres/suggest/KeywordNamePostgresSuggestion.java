package com.bioinformatics.dashboard.providers.postgres.suggest;

import com.bioinformatics.common.gene.repository.KeywordRepository;
import com.bioinformatics.common.providers.postgres.AbstractPostgresProvider;
import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PostgreSQL suggestion provider for keyword names.
 */
@Component
@RequiredArgsConstructor
public class KeywordNamePostgresSuggestion extends AbstractPostgresProvider implements SuggestionService {

    private final KeywordRepository repository;

    @Override
    public String field() {
        return "KeywordName";
    }

    @Override
    public List<String> suggest(String query) {
        return repository.findTop10ByNameContainingIgnoreCase(query);

    }


}

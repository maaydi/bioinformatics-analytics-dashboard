package com.bioinformatics.dashboard.providers.postgres.suggest;

import com.bioinformatics.common.gene.repository.CrossReferenceRepository;
import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import com.bioinformatics.dashboard.providers.postgres.AbstractPostgresProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PostgreSQL suggestion provider for cross-reference sources.
 */
@Component
@RequiredArgsConstructor
public class CrossReferencePostgresSuggestion extends AbstractPostgresProvider implements SuggestionService {

    private final CrossReferenceRepository repository;

    @Override
    public String field() {
        return "CrossReferenceSource";
    }

    @Override
    public List<String> suggest(String query) {
        return repository.findTop10BySourceContainingIgnoreCase(query);

    }


}

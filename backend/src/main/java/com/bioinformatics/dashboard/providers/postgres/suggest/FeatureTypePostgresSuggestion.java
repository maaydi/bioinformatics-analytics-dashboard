package com.bioinformatics.dashboard.providers.postgres.suggest;

import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import com.bioinformatics.dashboard.providers.postgres.AbstractPostgresProvider;
import com.bioinformatics.dashboard.providers.postgres.gene.repository.ProteinFeatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PostgreSQL suggestion provider for protein feature types.
 */
@Component
@RequiredArgsConstructor
public class FeatureTypePostgresSuggestion extends AbstractPostgresProvider implements SuggestionService {

    private final ProteinFeatureRepository repository;

    @Override
    public String field() {
        return "FeatureType";
    }

    @Override
    public List<String> suggest(String query) {
        return repository.findTop10ByFeatureTypeContainingIgnoreCase(query);
    }


}

package com.bioinformatics.dashboard.providers.postgres.suggest;

import com.bioinformatics.common.gene.repository.ProteinEntryRepository;
import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import com.bioinformatics.dashboard.providers.postgres.AbstractPostgresProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PostgreSQL suggestion provider for primary gene names.
 */
@Component
@RequiredArgsConstructor
public class GeneNamePrimaryPostgresSuggestion extends AbstractPostgresProvider implements SuggestionService {

    private final ProteinEntryRepository repository;

    @Override
    public String field() {
        return "GeneNamePrimary";
    }

    @Override
    public List<String> suggest(String query) {
        return repository.findTop10ByGeneNamePrimaryContainingIgnoreCase(query);

    }


}

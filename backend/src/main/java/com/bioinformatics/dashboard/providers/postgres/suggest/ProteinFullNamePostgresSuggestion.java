package com.bioinformatics.dashboard.providers.postgres.suggest;

import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import com.bioinformatics.dashboard.providers.postgres.AbstractPostgresProvider;
import com.bioinformatics.dashboard.providers.postgres.gene.repository.ProteinEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PostgreSQL suggestion provider for protein full names.
 */
@Component
@RequiredArgsConstructor
public class ProteinFullNamePostgresSuggestion extends AbstractPostgresProvider implements SuggestionService {

    private final ProteinEntryRepository repository;

    @Override
    public String field() {
        return "ProteinFullName";
    }

    @Override
    public List<String> suggest(String query) {
        return repository.findTop10ByProteinFullNameContainingIgnoreCase(query);

    }


}

package com.bioinformatics.dashboard.providers.postgres.suggest;

import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import com.bioinformatics.dashboard.providers.postgres.AbstractPostgresProvider;
import com.bioinformatics.dashboard.providers.postgres.gene.repository.ProteinEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

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

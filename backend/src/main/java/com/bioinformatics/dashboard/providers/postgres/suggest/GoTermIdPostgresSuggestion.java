package com.bioinformatics.dashboard.providers.postgres.suggest;

import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import com.bioinformatics.dashboard.providers.postgres.AbstractPostgresProvider;
import com.bioinformatics.dashboard.providers.postgres.gene.repository.GoTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GoTermIdPostgresSuggestion extends AbstractPostgresProvider implements SuggestionService {

    private final GoTermRepository repository;

    @Override
    public String field() {
        return "GoTermId";
    }

    @Override
    public List<String> suggest(String query) {
        return repository.findTop10ByGoIdContainingIgnoreCase(query);

    }


}

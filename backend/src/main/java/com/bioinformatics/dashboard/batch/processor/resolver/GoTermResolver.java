package com.bioinformatics.dashboard.batch.processor.resolver;

import com.bioinformatics.dashboard.providers.postgres.gene.entity.GoTerm;
import com.bioinformatics.dashboard.providers.postgres.gene.repository.GoTermRepository;
import jakarta.annotation.PostConstruct;
import lombok.Locked;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves and caches GoTerm entities. Ensures uniqueness and persists any
 * new goTerm encountered during UniProt import.
 */
@Component
@RequiredArgsConstructor
public class GoTermResolver {
    private final GoTermRepository goTermRepository;
    private final Map<String, GoTerm> goTermCache = new ConcurrentHashMap<>();


    @PostConstruct
    public void init() {
        goTermRepository.findAll()
                .forEach(goTerm -> goTermCache.put(goTerm.getGoId(), goTerm));
    }

    @Transactional
    @Locked.Write
    public Set<GoTerm> resolveGoTerms(final Set<GoTerm> goTermSet) {
        var result = new HashSet<GoTerm>();
        var newTerms = new HashSet<GoTerm>();
        for (var go : goTermSet) {
            var g = goTermCache.get(go.getGoId());
            if (Objects.nonNull(g)) {
                result.add(g);
            } else {
                newTerms.add(go);
            }
        }
        if (newTerms.isEmpty()) {
            return result;
        }
        var saved = goTermRepository.saveAll(newTerms);
        saved.forEach(e -> goTermCache.put(e.getGoId(), e));
        result.addAll(saved);

        return result;
    }
}

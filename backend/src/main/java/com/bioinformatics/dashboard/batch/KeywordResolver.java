package com.bioinformatics.dashboard.batch;

import com.bioinformatics.dashboard.gene.entity.Keyword;
import com.bioinformatics.dashboard.gene.repository.KeywordRepository;
import jakarta.annotation.PostConstruct;
import lombok.Locked;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This class resolve the uniqueness of keywords while processing uniprot
 * It use cache to keep keywords in memory
 */
@Component
@RequiredArgsConstructor
public class KeywordResolver {
    private final KeywordRepository keywordRepository;
    private final Map<String, Keyword> keywordCache = new ConcurrentHashMap<>();


    @PostConstruct
    public void init() {
        keywordRepository.findAll()
                .forEach(keyword -> keywordCache.put(keyword.getName(), keyword));
    }

    @Transactional
    @Locked.Write
    public List<Keyword> resolveKeywords(final List<Keyword> dataKeywords) {
        var names = dataKeywords.stream().map(Keyword::getName).collect(Collectors.toSet());
        var result = new ArrayList<Keyword>();
        var newKeys = new ArrayList<Keyword>();
        for (var name : names) {
            var k = keywordCache.get(name);
            if (Objects.nonNull(k)) {
                result.add(k);
            } else {
                newKeys.add(Keyword.builder().name(name).build());
            }
        }
        if (newKeys.isEmpty()) {
            return result;
        }
        var saved = keywordRepository.saveAll(newKeys);
        saved.forEach(e -> keywordCache.put(e.getName(), e));
        result.addAll(saved);

        return result;
    }
}

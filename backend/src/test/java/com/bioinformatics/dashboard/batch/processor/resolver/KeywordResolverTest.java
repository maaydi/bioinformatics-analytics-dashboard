package com.bioinformatics.dashboard.batch.processor.resolver;

import com.bioinformatics.dashboard.job.resolver.KeywordResolver;
import com.bioinformatics.dashboard.providers.postgres.gene.entity.Keyword;
import com.bioinformatics.dashboard.providers.postgres.gene.repository.KeywordRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")

public class KeywordResolverTest {

    @Test
    void initLoadsCacheAnd_resolveReturnsCachedKeywords() {
        var repo = mock(KeywordRepository.class);
        when(repo.findAll()).thenReturn(List.of(
                Keyword.builder().id(1).name("Alpha").build(),
                Keyword.builder().id(2).name("Beta").build()
        ));

        var resolver = new KeywordResolver(repo);
        resolver.init();

        // request a keyword that already exists in cache
        var requested = List.of(Keyword.builder().name("Alpha").build());
        var result = resolver.resolveKeywords(requested);

        verify(repo, never()).saveAll(any());
        assertEquals(1, result.size());
        assertEquals("Alpha", result.getFirst().getName());
    }

    @Test
    void resolveSavesAndReturnsNewKeywords() {
        var repo = mock(KeywordRepository.class);
        when(repo.findAll()).thenReturn(List.of());

        // Simulate DB assigning ids when saving
        when(repo.saveAll(any())).thenAnswer(invocation -> {
            var arg = (List<Keyword>) invocation.getArgument(0);
            // emulate persistence by setting ids
            return List.of(
                    Keyword.builder().id(10).name(arg.get(0).getName()).build(),
                    Keyword.builder().id(11).name(arg.get(1).getName()).build()
            );
        });

        var resolver = new KeywordResolver(repo);
        resolver.init();

        var toResolve = List.of(
                Keyword.builder().name("NewA").build(),
                Keyword.builder().name("NewB").build()
        );

        var res = resolver.resolveKeywords(toResolve);

        verify(repo, times(1)).saveAll(any());
        assertEquals(2, res.size());
        assertTrue(res.stream().anyMatch(k -> "NewA".equals(k.getName())));
        assertTrue(res.stream().anyMatch(k -> "NewB".equals(k.getName())));
    }

    @Test
    void resolveWithMixedExistingAndNew_callsSaveOnlyForNewOnes() {
        var repo = mock(KeywordRepository.class);
        when(repo.findAll()).thenReturn(List.of(Keyword.builder().id(1).name("Exist").build()));

        when(repo.saveAll(any())).thenAnswer(invocation -> {
            var arg = (List<Keyword>) invocation.getArgument(0);
            return arg.stream().map(k -> Keyword.builder().id(20).name(k.getName()).build()).toList();
        });

        var resolver = new KeywordResolver(repo);
        resolver.init();

        var toResolve = List.of(
                Keyword.builder().name("Exist").build(),
                Keyword.builder().name("BrandNew").build()
        );

        var res = resolver.resolveKeywords(toResolve);

        // saveAll should be called only for the single new name
        verify(repo, times(1)).saveAll(argThat(iter -> {
            int c = 0;
            String n = null;
            for (Keyword k : iter) {
                c++;
                n = k.getName();
            }
            return c == 1 && "BrandNew".equals(n);
        }));
        assertEquals(2, res.size());
        assertTrue(res.stream().anyMatch(k -> "Exist".equals(k.getName())));
        assertTrue(res.stream().anyMatch(k -> "BrandNew".equals(k.getName())));
    }

    @Test
    void resolveDeduplicatesInputAndSavesOnlyOnce() {
        var repo = mock(KeywordRepository.class);
        when(repo.findAll()).thenReturn(List.of());

        when(repo.saveAll(any())).thenAnswer(invocation -> {
            var arg = (List<Keyword>) invocation.getArgument(0);
            return arg.stream().map(k -> Keyword.builder().id(30).name(k.getName()).build()).toList();
        });

        var resolver = new KeywordResolver(repo);
        resolver.init();

        // duplicate names in input
        var toResolve = List.of(
                Keyword.builder().name("Dup").build(),
                Keyword.builder().name("Dup").build()
        );

        var res = resolver.resolveKeywords(toResolve);

        // only one persisted entity should be requested
        verify(repo, times(1)).saveAll(argThat(iter -> {
            int c = 0;
            for (Keyword k : iter) c++;
            return c == 1;
        }));
        assertEquals(1, res.size());
        assertEquals("Dup", res.getFirst().getName());
    }
}


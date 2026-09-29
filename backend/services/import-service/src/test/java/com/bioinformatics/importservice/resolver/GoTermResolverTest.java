package com.bioinformatics.importservice.resolver;

import com.bioinformatics.common.gene.entity.GoTerm;
import com.bioinformatics.common.gene.repository.GoTermRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoTermResolverTest {

    @Mock
    private GoTermRepository goTermRepository;

    private GoTermResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new GoTermResolver(goTermRepository);
    }

    @Test
    void initLoadsExistingTermsIntoCache() {
        var existing = GoTerm.builder()
                .id(1)
                .goId("GO:0008150")
                .aspect('P')
                .description("biological_process")
                .build();

        when(goTermRepository.findAll()).thenReturn(List.of(existing));

        resolver.init();

        var result = resolver.resolveGoTerms(Set.of(existing));

        assertThat(result).containsExactly(existing);
        verify(goTermRepository, never()).saveAll(any());
    }

    @Test
    void resolveGoTermsPersistsOnlyNewTerms() {
        var existing = GoTerm.builder()
                .id(1)
                .goId("GO:0008150")
                .aspect('P')
                .description("existing")
                .build();
        var newTerm1 = GoTerm.builder()
                .goId("GO:0003674")
                .aspect('F')
                .description("molecular_function")
                .build();
        var newTerm2 = GoTerm.builder()
                .goId("GO:0005575")
                .aspect('C')
                .description("cellular_component")
                .build();

        when(goTermRepository.findAll()).thenReturn(List.of(existing));
        when(goTermRepository.saveAll(any())).thenReturn(List.of(
                GoTerm.builder().id(2).goId("GO:0003674").aspect('F').description("molecular_function").build(),
                GoTerm.builder().id(3).goId("GO:0005575").aspect('C').description("cellular_component").build()
        ));

        resolver.init();

        var result = resolver.resolveGoTerms(Set.of(existing, newTerm1, newTerm2));

        assertThat(result)
                .extracting(GoTerm::getGoId)
                .containsExactlyInAnyOrder("GO:0008150", "GO:0003674", "GO:0005575");
        verify(goTermRepository).saveAll(any());
    }
}


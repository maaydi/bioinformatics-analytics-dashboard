package com.bioinformatics.dashboard.providers.postgres.suggest;

import com.bioinformatics.dashboard.providers.postgres.gene.repository.ProteinFeatureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureTypePostgresSuggestionTest {

    @Mock
    private ProteinFeatureRepository repository;

    private FeatureTypePostgresSuggestion suggestion;

    @BeforeEach
    void setUp() {
        suggestion = new FeatureTypePostgresSuggestion(repository);
    }

    // -------------------------------------------------------------------------
    // field()
    // -------------------------------------------------------------------------

    @Test
    void field_returnsFeatureType() {
        assertThat(suggestion.field()).isEqualTo("FeatureType");
    }

    // -------------------------------------------------------------------------
    // getProviderName() — inherited from AbstractPostgresProvider
    // -------------------------------------------------------------------------

    @Test
    void getProviderName_returnsPostgres() {
        assertThat(suggestion.getProviderName()).isEqualTo("postgres");
    }

    // -------------------------------------------------------------------------
    // suggest(String)
    // -------------------------------------------------------------------------

    @Test
    void suggest_delegatesToRepository() {
        var query = "chain";
        var expected = List.of("Chain", "Chain (Propeptide)");
        when(repository.findTop10ByFeatureTypeContainingIgnoreCase(query)).thenReturn(expected);

        var result = suggestion.suggest(query);

        assertThat(result).isEqualTo(expected);
        verify(repository).findTop10ByFeatureTypeContainingIgnoreCase(query);
    }

    @Test
    void suggest_emptyQuery_returnsRepositoryResult() {
        var expected = List.of("Active site", "Binding site", "Chain");
        when(repository.findTop10ByFeatureTypeContainingIgnoreCase("")).thenReturn(expected);

        var result = suggestion.suggest("");

        assertThat(result).hasSize(3);
    }

    @Test
    void suggest_noMatches_returnsEmptyList() {
        when(repository.findTop10ByFeatureTypeContainingIgnoreCase("XXXXXX")).thenReturn(List.of());

        var result = suggestion.suggest("XXXXXX");

        assertThat(result).isEmpty();
    }

    @Test
    void suggest_returnsAtMostTenResults() {
        var tenResults = List.of(
                "Active site", "Binding site", "Chain", "Coiled coil",
                "Compositional bias", "Cross-link", "Disulfide bond", "DNA binding",
                "Glycosylation", "Initiator methionine"
        );
        when(repository.findTop10ByFeatureTypeContainingIgnoreCase("site")).thenReturn(tenResults);

        var result = suggestion.suggest("site");

        assertThat(result).hasSize(10);
    }

    // -------------------------------------------------------------------------
    // suggest(String field, String query) — default interface method delegation
    // -------------------------------------------------------------------------

    @Test
    void suggest_withField_delegatesToSuggestQuery() {
        var query = "bind";
        var expected = List.of("Binding site");
        when(repository.findTop10ByFeatureTypeContainingIgnoreCase(query)).thenReturn(expected);

        var result = suggestion.suggest("FeatureType", query);

        assertThat(result).isEqualTo(expected);
    }
}


package com.bioinformatics.dashboard.providers.postgres.suggest;

import com.bioinformatics.common.gene.repository.ProteinEntryRepository;
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
class GeneNamePrimaryPostgresSuggestionTest {

    @Mock
    private ProteinEntryRepository repository;

    private GeneNamePrimaryPostgresSuggestion suggestion;

    @BeforeEach
    void setUp() {
        suggestion = new GeneNamePrimaryPostgresSuggestion(repository);
    }

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    @Test
    void field_returnsGeneNamePrimary() {
        assertThat(suggestion.field()).isEqualTo("GeneNamePrimary");
    }

    @Test
    void getProviderName_returnsPostgres() {
        assertThat(suggestion.getProviderName()).isEqualTo("postgres");
    }

    // -------------------------------------------------------------------------
    // suggest(String)
    // -------------------------------------------------------------------------

    @Test
    void suggest_delegatesToRepository() {
        var query = "BRCA";
        var expected = List.of("BRCA1", "BRCA2");
        when(repository.findTop10ByGeneNamePrimaryContainingIgnoreCase(query)).thenReturn(expected);

        var result = suggestion.suggest(query);

        assertThat(result).isEqualTo(expected);
        verify(repository).findTop10ByGeneNamePrimaryContainingIgnoreCase(query);
    }

    @Test
    void suggest_emptyQuery_passesEmptyStringToRepository() {
        var expected = List.of("TP53", "EGFR");
        when(repository.findTop10ByGeneNamePrimaryContainingIgnoreCase("")).thenReturn(expected);

        var result = suggestion.suggest("");

        assertThat(result).containsExactlyElementsOf(expected);
        verify(repository).findTop10ByGeneNamePrimaryContainingIgnoreCase("");
    }

    @Test
    void suggest_noMatches_returnsEmptyList() {
        when(repository.findTop10ByGeneNamePrimaryContainingIgnoreCase("ZZZUNKNOWN")).thenReturn(List.of());

        var result = suggestion.suggest("ZZZUNKNOWN");

        assertThat(result).isEmpty();
    }

    @Test
    void suggest_returnsAtMostTenResults() {
        var tenResults = List.of(
                "TP53", "EGFR", "KRAS", "BRCA1", "BRCA2",
                "MYC", "PTEN", "RB1", "APC", "VHL"
        );
        when(repository.findTop10ByGeneNamePrimaryContainingIgnoreCase("")).thenReturn(tenResults);

        var result = suggestion.suggest("");

        assertThat(result).hasSize(10);
    }

    // -------------------------------------------------------------------------
    // suggest(String field, String query) — default interface method delegation
    // -------------------------------------------------------------------------

    @Test
    void suggest_withFieldParam_delegatesToSuggestQuery() {
        var query = "KRAS";
        var expected = List.of("KRAS");
        when(repository.findTop10ByGeneNamePrimaryContainingIgnoreCase(query)).thenReturn(expected);

        var result = suggestion.suggest("GeneNamePrimary", query);

        assertThat(result).isEqualTo(expected);
    }
}


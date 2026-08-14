package com.bioinformatics.dashboard.providers.postgres.suggest;

import com.bioinformatics.dashboard.providers.postgres.gene.repository.ProteinEntryRepository;
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
class EntryNamePostgresSuggestionTest {

    @Mock
    private ProteinEntryRepository repository;

    private EntryNamePostgresSuggestion suggestion;

    @BeforeEach
    void setUp() {
        suggestion = new EntryNamePostgresSuggestion(repository);
    }

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    @Test
    void field_returnsEntryName() {
        assertThat(suggestion.field()).isEqualTo("EntryName");
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
        var query = "TP53";
        var expected = List.of("TP53_HUMAN", "TP53_MOUSE");
        when(repository.findTop10ByEntryNameContainingIgnoreCase(query)).thenReturn(expected);

        var result = suggestion.suggest(query);

        assertThat(result).isEqualTo(expected);
        verify(repository).findTop10ByEntryNameContainingIgnoreCase(query);
    }

    @Test
    void suggest_emptyQuery_passesEmptyStringToRepository() {
        var expected = List.of("BRCA1_HUMAN", "EGFR_HUMAN");
        when(repository.findTop10ByEntryNameContainingIgnoreCase("")).thenReturn(expected);

        var result = suggestion.suggest("");

        assertThat(result).containsExactlyElementsOf(expected);
        verify(repository).findTop10ByEntryNameContainingIgnoreCase("");
    }

    @Test
    void suggest_noMatches_returnsEmptyList() {
        when(repository.findTop10ByEntryNameContainingIgnoreCase("ZZZUNKNOWN")).thenReturn(List.of());

        var result = suggestion.suggest("ZZZUNKNOWN");

        assertThat(result).isEmpty();
    }

    @Test
    void suggest_returnsAtMostTenResults() {
        var tenResults = List.of(
                "P53_HUMAN", "P53_MOUSE", "P53_RAT", "P53_PIG", "P53_HORSE",
                "P53_SHEEP", "P53_BOVIN", "P53_CHICK", "P53_XENLA", "P53_DANRE"
        );
        when(repository.findTop10ByEntryNameContainingIgnoreCase("P53")).thenReturn(tenResults);

        var result = suggestion.suggest("P53");

        assertThat(result).hasSize(10);
    }

    // -------------------------------------------------------------------------
    // suggest(String field, String query) — default interface method delegation
    // -------------------------------------------------------------------------

    @Test
    void suggest_withFieldParam_delegatesToSuggestQuery() {
        var query = "BRCA";
        var expected = List.of("BRCA1_HUMAN", "BRCA2_HUMAN");
        when(repository.findTop10ByEntryNameContainingIgnoreCase(query)).thenReturn(expected);

        var result = suggestion.suggest("EntryName", query);

        assertThat(result).isEqualTo(expected);
    }
}


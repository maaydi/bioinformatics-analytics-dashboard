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
class AccessionPostgresSuggestionTest {

    @Mock
    private ProteinEntryRepository repository;

    private AccessionPostgresSuggestion suggestion;

    @BeforeEach
    void setUp() {
        suggestion = new AccessionPostgresSuggestion(repository);
    }

    // -------------------------------------------------------------------------
    // field()
    // -------------------------------------------------------------------------

    @Test
    void field_returnsAccession() {
        assertThat(suggestion.field()).isEqualTo("Accession");
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
        var query = "P123";
        var expected = List.of("P12345", "P12367");
        when(repository.findTop10ByAccessionContainingIgnoreCase(query)).thenReturn(expected);

        var result = suggestion.suggest(query);

        assertThat(result).isEqualTo(expected);
        verify(repository).findTop10ByAccessionContainingIgnoreCase(query);
    }

    @Test
    void suggest_emptyQuery_returnsRepositoryResult() {
        var expected = List.of("A0A001", "A0B002");
        when(repository.findTop10ByAccessionContainingIgnoreCase("")).thenReturn(expected);

        var result = suggestion.suggest("");

        assertThat(result).hasSize(2);
    }

    @Test
    void suggest_noMatches_returnsEmptyList() {
        when(repository.findTop10ByAccessionContainingIgnoreCase("XXXXXX")).thenReturn(List.of());

        var result = suggestion.suggest("XXXXXX");

        assertThat(result).isEmpty();
    }

    @Test
    void suggest_returnsAtMostTenResults() {
        var tenResults = List.of("P00001", "P00002", "P00003", "P00004", "P00005",
                "P00006", "P00007", "P00008", "P00009", "P00010");
        when(repository.findTop10ByAccessionContainingIgnoreCase("P")).thenReturn(tenResults);

        var result = suggestion.suggest("P");

        assertThat(result).hasSize(10);
    }

    // -------------------------------------------------------------------------
    // suggest(String field, String query) — default interface method delegation
    // -------------------------------------------------------------------------

    @Test
    void suggest_withField_delegatesToSuggestQuery() {
        var query = "Q9";
        var expected = List.of("Q9Y2J2");
        when(repository.findTop10ByAccessionContainingIgnoreCase(query)).thenReturn(expected);

        var result = suggestion.suggest("Accession", query);

        assertThat(result).isEqualTo(expected);
    }
}


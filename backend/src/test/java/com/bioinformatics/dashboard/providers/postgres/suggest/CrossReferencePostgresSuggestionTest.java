package com.bioinformatics.dashboard.providers.postgres.suggest;

import com.bioinformatics.dashboard.providers.postgres.gene.repository.CrossReferenceRepository;
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
class CrossReferencePostgresSuggestionTest {

    @Mock
    private CrossReferenceRepository repository;

    private CrossReferencePostgresSuggestion suggestion;

    @BeforeEach
    void setUp() {
        suggestion = new CrossReferencePostgresSuggestion(repository);
    }

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    @Test
    void field_returnsCrossReferenceSource() {
        assertThat(suggestion.field()).isEqualTo("CrossReferenceSource");
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
        var query = "PDB";
        var expected = List.of("PDB", "PDBE");
        when(repository.findTop10BySourceContainingIgnoreCase(query)).thenReturn(expected);

        var result = suggestion.suggest(query);

        assertThat(result).isEqualTo(expected);
        verify(repository).findTop10BySourceContainingIgnoreCase(query);
    }

    @Test
    void suggest_emptyQuery_passesEmptyStringToRepository() {
        var expected = List.of("RefSeq", "UniRef");
        when(repository.findTop10BySourceContainingIgnoreCase("")).thenReturn(expected);

        var result = suggestion.suggest("");

        assertThat(result).containsExactlyElementsOf(expected);
        verify(repository).findTop10BySourceContainingIgnoreCase("");
    }

    @Test
    void suggest_noMatches_returnsEmptyList() {
        when(repository.findTop10BySourceContainingIgnoreCase("UNKNOWN")).thenReturn(List.of());

        var result = suggestion.suggest("UNKNOWN");

        assertThat(result).isEmpty();
    }

    @Test
    void suggest_returnsAtMostTenResults() {
        var tenResults = List.of("PDB", "PDBE", "RefSeq", "UniRef50", "UniRef90",
                "UniRef100", "UniParc", "EMBL", "CCDS", "ChEMBL");
        when(repository.findTop10BySourceContainingIgnoreCase("P")).thenReturn(tenResults);

        var result = suggestion.suggest("P");

        assertThat(result).hasSize(10);
    }

    // -------------------------------------------------------------------------
    // suggest(String field, String query) — default interface method delegation
    // -------------------------------------------------------------------------

    @Test
    void suggest_withFieldParam_delegatesToSuggestQuery() {
        var query = "Ens";
        var expected = List.of("Ensembl");
        when(repository.findTop10BySourceContainingIgnoreCase(query)).thenReturn(expected);

        var result = suggestion.suggest("CrossReferenceSource", query);

        assertThat(result).isEqualTo(expected);
    }
}


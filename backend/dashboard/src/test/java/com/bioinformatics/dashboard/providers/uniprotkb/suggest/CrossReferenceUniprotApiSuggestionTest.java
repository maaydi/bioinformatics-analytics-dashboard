package com.bioinformatics.dashboard.providers.uniprotkb.suggest;

import com.bioinformatics.common.providers.uniprotkb.dto.CrossRefLightEntry;
import com.bioinformatics.common.providers.uniprotkb.service.DatabaseRestService;
import com.bioinformatics.common.uniprot.dto.UniprotKbResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrossReferenceUniprotApiSuggestionTest {

    @Mock
    private DatabaseRestService databaseRestService;

    private CrossReferenceUniprotApiSuggestion suggestion;

    @BeforeEach
    void setUp() {
        suggestion = new CrossReferenceUniprotApiSuggestion(databaseRestService);
    }

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    @Test
    void field_returnsCrossReferenceSource() {
        assertThat(suggestion.field()).isEqualTo("CrossReferenceSource");
    }

    @Test
    void getProviderName_returnsUniprotKb() {
        assertThat(suggestion.getProviderName()).isEqualTo("uniprotKb");
    }

    // -------------------------------------------------------------------------
    // suggest(String) — happy path
    // -------------------------------------------------------------------------

    @Test
    void suggest_returnsAbbrevListFromApiResponse() {
        var entries = List.of(new CrossRefLightEntry("PDB"), new CrossRefLightEntry("PDBE"));
        var body = new UniprotKbResponse<>(entries);
        when(databaseRestService.searchAll(anyString(), anyInt()))
                .thenReturn(ResponseEntity.ok(body));

        var result = suggestion.suggest("PDB");

        assertThat(result).containsExactly("PDB", "PDBE");
    }

    @Test
    void suggest_buildsCorrectQueryFormatAndPageSize() {
        var body = new UniprotKbResponse<>(List.<CrossRefLightEntry>of());
        when(databaseRestService.searchAll("((name:Ens*))", 50))
                .thenReturn(ResponseEntity.ok(body));

        suggestion.suggest("Ens");

        verify(databaseRestService).searchAll("((name:Ens*))", 50);
    }

    @Test
    void suggest_limitsResultsToTen_whenApiReturnsMore() {
        // 15 distinct abbreviations — only 10 must be returned
        var entries = IntStream.rangeClosed(1, 15)
                .mapToObj(i -> new CrossRefLightEntry("DB" + i))
                .toList();
        var body = new UniprotKbResponse<>(entries);
        when(databaseRestService.searchAll(anyString(), anyInt()))
                .thenReturn(ResponseEntity.ok(body));

        var result = suggestion.suggest("DB");

        assertThat(result).hasSize(10);
    }

    @Test
    void suggest_deduplicatesAbbrevs_beforeLimit() {
        // Same abbreviation repeated — distinct() must collapse them
        var entries = List.of(
                new CrossRefLightEntry("PDB"),
                new CrossRefLightEntry("PDB"),
                new CrossRefLightEntry("EMBL")
        );
        var body = new UniprotKbResponse<>(entries);
        when(databaseRestService.searchAll(anyString(), anyInt()))
                .thenReturn(ResponseEntity.ok(body));

        var result = suggestion.suggest("P");

        assertThat(result).containsExactlyInAnyOrder("PDB", "EMBL");
        assertThat(result).doesNotHaveDuplicates();
    }

    @Test
    void suggest_emptyResults_returnsEmptyList() {
        var body = new UniprotKbResponse<>(List.<CrossRefLightEntry>of());
        when(databaseRestService.searchAll(anyString(), anyInt()))
                .thenReturn(ResponseEntity.ok(body));

        var result = suggestion.suggest("ZZZ");

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // suggest(String) — null / no-body response
    // -------------------------------------------------------------------------

    @Test
    void suggest_nullBody_returnsEmptyList() {
        when(databaseRestService.searchAll(anyString(), anyInt()))
                .thenReturn(ResponseEntity.ok(null));

        var result = suggestion.suggest("PDB");

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // suggest(String) — error path (API throws)
    // -------------------------------------------------------------------------

    @Test
    void suggest_apiThrowsRuntimeException_returnsEmptyList() {
        when(databaseRestService.searchAll(anyString(), anyInt()))
                .thenThrow(new RuntimeException("Connection refused"));

        var result = suggestion.suggest("PDB");

        assertThat(result).isEmpty();
    }

    @Test
    void suggest_apiThrowsCheckedWrappedException_returnsEmptyList() {
        when(databaseRestService.searchAll(anyString(), anyInt()))
                .thenThrow(new IllegalStateException("Unexpected API state"));

        var result = suggestion.suggest("UniRef");

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // suggest(String field, String query) — default interface method delegation
    // -------------------------------------------------------------------------

    @Test
    void suggest_withFieldParam_delegatesToSuggestQuery() {
        var entries = List.of(new CrossRefLightEntry("Ensembl"));
        var body = new UniprotKbResponse<>(entries);
        when(databaseRestService.searchAll(anyString(), anyInt()))
                .thenReturn(ResponseEntity.ok(body));

        var result = suggestion.suggest("CrossReferenceSource", "Ens");

        assertThat(result).containsExactly("Ensembl");
    }
}


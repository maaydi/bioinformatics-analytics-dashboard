package com.bioinformatics.dashboard.providers.uniprotkb.suggest;

import com.bioinformatics.dashboard.model.uniprot.dto.UniprotKbResponse;
import com.bioinformatics.dashboard.providers.uniprotkb.dto.UniProtLightEntry;
import com.bioinformatics.dashboard.providers.uniprotkb.service.UniprotKbRestService;
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
class EntryNameUniprotApiSuggestionTest {

    @Mock
    private UniprotKbRestService uniprotKbRestService;

    private EntryNameUniprotApiSuggestion suggestion;

    private static UniProtLightEntry lightEntry(String uniProtkbId) {
        return new UniProtLightEntry(uniProtkbId, null, null, null);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static ResponseEntity<UniprotKbResponse<UniProtLightEntry>> okResponse(List<UniProtLightEntry> entries) {
        return ResponseEntity.ok(new UniprotKbResponse<>(entries));
    }

    @BeforeEach
    void setUp() {
        suggestion = new EntryNameUniprotApiSuggestion(uniprotKbRestService);
    }

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    @Test
    void field_returnsEntryName() {
        assertThat(suggestion.field()).isEqualTo("EntryName");
    }

    @Test
    void getProviderName_returnsUniprotKb() {
        assertThat(suggestion.getProviderName()).isEqualTo("uniprotKb");
    }

    // -------------------------------------------------------------------------
    // suggest(String) — happy path
    // -------------------------------------------------------------------------

    @Test
    void suggest_returnsUniProtkbIdListFromApiResponse() {
        var entries = List.of(lightEntry("TP53_HUMAN"), lightEntry("TP53_MOUSE"));
        when(uniprotKbRestService.searchAll(anyString(), anyInt())).thenReturn(okResponse(entries));

        var result = suggestion.suggest("TP53");

        assertThat(result).containsExactly("TP53_HUMAN", "TP53_MOUSE");
    }

    @Test
    void suggest_buildsCorrectQueryFormatAndPageSize() {
        when(uniprotKbRestService.searchAll("((id:EGFR*))", 50))
                .thenReturn(okResponse(List.of()));

        suggestion.suggest("EGFR");

        verify(uniprotKbRestService).searchAll("((id:EGFR*))", 50);
    }

    @Test
    void suggest_limitsResultsToTen_whenApiReturnsMore() {
        var entries = IntStream.rangeClosed(1, 15)
                .mapToObj(i -> lightEntry("GENE" + i + "_HUMAN"))
                .toList();
        when(uniprotKbRestService.searchAll(anyString(), anyInt())).thenReturn(okResponse(entries));

        var result = suggestion.suggest("GENE");

        assertThat(result).hasSize(10);
    }

    @Test
    void suggest_deduplicatesIds_beforeLimit() {
        var entries = List.of(
                lightEntry("BRCA1_HUMAN"),
                lightEntry("BRCA1_HUMAN"),
                lightEntry("BRCA2_HUMAN")
        );
        when(uniprotKbRestService.searchAll(anyString(), anyInt())).thenReturn(okResponse(entries));

        var result = suggestion.suggest("BRCA");

        assertThat(result).containsExactlyInAnyOrder("BRCA1_HUMAN", "BRCA2_HUMAN");
        assertThat(result).doesNotHaveDuplicates();
    }

    @Test
    void suggest_emptyResults_returnsEmptyList() {
        when(uniprotKbRestService.searchAll(anyString(), anyInt())).thenReturn(okResponse(List.of()));

        var result = suggestion.suggest("ZZZUNKNOWN");

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // suggest(String) — null / no-body response
    // -------------------------------------------------------------------------

    @Test
    void suggest_nullBody_returnsEmptyList() {
        when(uniprotKbRestService.searchAll(anyString(), anyInt()))
                .thenReturn(ResponseEntity.ok(null));

        var result = suggestion.suggest("TP53");

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // suggest(String) — error path
    // -------------------------------------------------------------------------

    @Test
    void suggest_apiThrowsRuntimeException_returnsEmptyList() {
        when(uniprotKbRestService.searchAll(anyString(), anyInt()))
                .thenThrow(new RuntimeException("Connection refused"));

        var result = suggestion.suggest("TP53");

        assertThat(result).isEmpty();
    }

    @Test
    void suggest_apiThrowsIllegalStateException_returnsEmptyList() {
        when(uniprotKbRestService.searchAll(anyString(), anyInt()))
                .thenThrow(new IllegalStateException("Unexpected state"));

        var result = suggestion.suggest("EGFR");

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // suggest(String field, String query) — default interface method delegation
    // -------------------------------------------------------------------------

    @Test
    void suggest_withFieldParam_delegatesToSuggestQuery() {
        var entries = List.of(lightEntry("KRAS_HUMAN"));
        when(uniprotKbRestService.searchAll(anyString(), anyInt())).thenReturn(okResponse(entries));

        var result = suggestion.suggest("EntryName", "KRAS");

        assertThat(result).containsExactly("KRAS_HUMAN");
    }
}


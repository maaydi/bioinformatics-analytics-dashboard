package com.bioinformatics.dashboard.providers.uniprotkb.suggest;

import com.bioinformatics.dashboard.model.uniprot.dto.GeneName;
import com.bioinformatics.dashboard.model.uniprot.dto.UniprotKbResponse;
import com.bioinformatics.dashboard.providers.uniprotkb.dto.GeneLight;
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
class GeneNamePrimaryUniprotApiSuggestionTest {

    @Mock
    private UniprotKbRestService uniprotKbRestService;

    private GeneNamePrimaryUniprotApiSuggestion suggestion;

    /**
     * Creates a GeneName with no evidences and the given value.
     */
    private static GeneName geneName(String value) {
        return new GeneName(List.of(), value);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a UniProtLightEntry whose first gene carries the given primary name.
     */
    private static UniProtLightEntry entryWithGeneName(String value) {
        var gene = new GeneLight(geneName(value));
        return new UniProtLightEntry(null, null, null, null, List.of(gene), null);
    }

    private static ResponseEntity<UniprotKbResponse<UniProtLightEntry>> okResponse(
            List<UniProtLightEntry> entries) {
        return ResponseEntity.ok(new UniprotKbResponse<>(entries));
    }

    @BeforeEach
    void setUp() {
        suggestion = new GeneNamePrimaryUniprotApiSuggestion(uniprotKbRestService);
    }

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    @Test
    void field_returnsGeneNamePrimary() {
        assertThat(suggestion.field()).isEqualTo("GeneNamePrimary");
    }

    @Test
    void getProviderName_returnsUniprotKb() {
        assertThat(suggestion.getProviderName()).isEqualTo("uniprotKb");
    }

    // -------------------------------------------------------------------------
    // suggest(String) — happy path
    // -------------------------------------------------------------------------

    @Test
    void suggest_returnsPrimaryGeneNamesFromApiResponse() {
        var entries = List.of(entryWithGeneName("BRCA1"), entryWithGeneName("BRCA2"));
        when(uniprotKbRestService.searchAll(anyString(), anyInt())).thenReturn(okResponse(entries));

        var result = suggestion.suggest("BRCA");

        assertThat(result).containsExactly("BRCA1", "BRCA2");
    }

    @Test
    void suggest_buildsCorrectQueryFormatAndPageSize() {
        when(uniprotKbRestService.searchAll("((gene:TP53*))", 50))
                .thenReturn(okResponse(List.of()));

        suggestion.suggest("TP53");

        verify(uniprotKbRestService).searchAll("((gene:TP53*))", 50);
    }

    @Test
    void suggest_limitsResultsToTen_whenApiReturnsMore() {
        var entries = IntStream.rangeClosed(1, 15)
                .mapToObj(i -> entryWithGeneName("GENE" + i))
                .toList();
        when(uniprotKbRestService.searchAll(anyString(), anyInt())).thenReturn(okResponse(entries));

        var result = suggestion.suggest("GENE");

        assertThat(result).hasSize(10);
    }

    @Test
    void suggest_deduplicatesPrimaryNames_beforeLimit() {
        var entries = List.of(
                entryWithGeneName("KRAS"),
                entryWithGeneName("KRAS"),
                entryWithGeneName("NRAS")
        );
        when(uniprotKbRestService.searchAll(anyString(), anyInt())).thenReturn(okResponse(entries));

        var result = suggestion.suggest("RAS");

        assertThat(result).containsExactlyInAnyOrder("KRAS", "NRAS");
        assertThat(result).doesNotHaveDuplicates();
    }

    @Test
    void suggest_emptyResults_returnsEmptyList() {
        when(uniprotKbRestService.searchAll(anyString(), anyInt())).thenReturn(okResponse(List.of()));

        var result = suggestion.suggest("ZZZUNKNOWN");

        assertThat(result).isEmpty();
    }

    @Test
    void suggest_entryWithNullGenesList_isSkipped() {
        // Entry with null genes list must be filtered out without NPE
        var entryWithNullGenes = new UniProtLightEntry(null, null, null, null, null, null);
        var entryWithValidGene = entryWithGeneName("EGFR");
        when(uniprotKbRestService.searchAll(anyString(), anyInt()))
                .thenReturn(okResponse(List.of(entryWithNullGenes, entryWithValidGene)));

        var result = suggestion.suggest("EG");

        assertThat(result).containsExactly("EGFR");
    }

    @Test
    void suggest_entryWithEmptyGenesList_isSkipped() {
        var entryWithEmptyGenes = new UniProtLightEntry(null, null, null, null, List.of(), null);
        var entryWithValidGene = entryWithGeneName("MYC");
        when(uniprotKbRestService.searchAll(anyString(), anyInt()))
                .thenReturn(okResponse(List.of(entryWithEmptyGenes, entryWithValidGene)));

        var result = suggestion.suggest("MY");

        assertThat(result).containsExactly("MYC");
    }

    @Test
    void suggest_geneWithNullGeneName_isSkipped() {
        // GeneLight whose geneName() is null must be filtered without NPE
        var geneWithNullName = new GeneLight(null);
        var entryWithNullGeneName = new UniProtLightEntry(null, null, null, null, List.of(geneWithNullName), null);
        var entryWithValidGene = entryWithGeneName("PTEN");
        when(uniprotKbRestService.searchAll(anyString(), anyInt()))
                .thenReturn(okResponse(List.of(entryWithNullGeneName, entryWithValidGene)));

        var result = suggestion.suggest("PT");

        assertThat(result).containsExactly("PTEN");
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

        var result = suggestion.suggest("BRCA1");

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
        var entries = List.of(entryWithGeneName("RB1"));
        when(uniprotKbRestService.searchAll(anyString(), anyInt())).thenReturn(okResponse(entries));

        var result = suggestion.suggest("GeneNamePrimary", "RB");

        assertThat(result).containsExactly("RB1");
    }
}


package com.bioinformatics.dashboard.providers.uniprotkb.suggest;

import com.bioinformatics.dashboard.model.uniprot.dto.FullName;
import com.bioinformatics.dashboard.model.uniprot.dto.ProteinDescription;
import com.bioinformatics.dashboard.model.uniprot.dto.RecommendedName;
import com.bioinformatics.dashboard.model.uniprot.dto.UniprotKbResponse;
import com.bioinformatics.dashboard.providers.uniprotkb.dto.UniProtLightEntry;
import com.bioinformatics.dashboard.providers.uniprotkb.service.UniprotKbRestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

/**
 * Unit tests for {@link ProteinFullNameUniprotApiSuggestion}.
 *
 * <p>Verifies correct protein full name extraction from the UniProt KB REST API,
 * query format, deduplication, limit enforcement, and graceful error handling.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProteinFullNameUniprotApiSuggestion")
class ProteinFullNameUniprotApiSuggestionTest {

    @Mock
    private UniprotKbRestService uniprotKbRestService;

    @InjectMocks
    private ProteinFullNameUniprotApiSuggestion suggestion;

    // ---- helpers ----------------------------------------------------------------

    private static UniProtLightEntry entryWithFullName(String name) {
        var fullName = new FullName(name);
        var recommendedName = new RecommendedName(fullName);
        var proteinDescription = new ProteinDescription(recommendedName);
        return new UniProtLightEntry(null, null, null, null, null, proteinDescription);
    }

    private static ResponseEntity<UniprotKbResponse<UniProtLightEntry>> okResponse(
            List<UniProtLightEntry> entries) {
        return ResponseEntity.ok(new UniprotKbResponse<>(entries));
    }

    // ---- tests ------------------------------------------------------------------

    @Nested
    @DisplayName("field()")
    class FieldMethod {

        @Test
        @DisplayName("should return 'ProteinFullName' as the target field name")
        void shouldReturnProteinFullNameAsFieldName() {
            assertThat(suggestion.field()).isEqualTo("ProteinFullName");
        }
    }

    @Nested
    @DisplayName("getProviderName()")
    class ProviderNameMethod {

        @Test
        @DisplayName("should return 'uniprotKb' as the provider name")
        void shouldReturnUniprotKbAsProviderName() {
            assertThat(suggestion.getProviderName()).isEqualTo("uniprotKb");
        }
    }

    @Nested
    @DisplayName("suggest(String query)")
    class SuggestMethod {

        @Test
        @DisplayName("should return protein full names extracted from the API response")
        void shouldReturnProteinFullNamesFromApiResponse() {
            // Arrange
            var query = "kinase";
            var entries = List.of(
                    entryWithFullName("Tyrosine-protein kinase ABL1"),
                    entryWithFullName("Serine/threonine-protein kinase ATM")
            );
            when(uniprotKbRestService.searchAll(anyString(), anyInt())).thenReturn(okResponse(entries));

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).containsExactly(
                    "Tyrosine-protein kinase ABL1",
                    "Serine/threonine-protein kinase ATM"
            );
        }

        @Test
        @DisplayName("should build the correct protein_name query format with wildcard and page size 50")
        void shouldBuildCorrectQueryFormatAndPageSize() {
            // Arrange
            when(uniprotKbRestService.searchAll("((protein_name:receptor*))", 50))
                    .thenReturn(okResponse(List.of()));

            // Act
            suggestion.suggest("receptor");

            // Assert
            verify(uniprotKbRestService).searchAll("((protein_name:receptor*))", 50);
        }

        @Test
        @DisplayName("should deduplicate protein full names before enforcing the limit")
        void shouldDeduplicateProteinFullNames() {
            // Arrange — same full name appears twice
            var query = "hemoglobin";
            var entries = List.of(
                    entryWithFullName("Hemoglobin subunit alpha"),
                    entryWithFullName("Hemoglobin subunit alpha"),
                    entryWithFullName("Hemoglobin subunit beta")
            );
            when(uniprotKbRestService.searchAll(anyString(), anyInt())).thenReturn(okResponse(entries));

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result)
                    .hasSize(2)
                    .containsExactlyInAnyOrder("Hemoglobin subunit alpha", "Hemoglobin subunit beta");
            assertThat(result).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("should return at most 10 suggestions when the API returns more")
        void shouldLimitResultsToTen() {
            // Arrange
            var entries = IntStream.rangeClosed(1, 15)
                    .mapToObj(i -> entryWithFullName("Protein " + i))
                    .toList();
            when(uniprotKbRestService.searchAll(anyString(), anyInt())).thenReturn(okResponse(entries));

            // Act
            var result = suggestion.suggest("protein");

            // Assert
            assertThat(result).hasSize(10);
        }

        @Test
        @DisplayName("should return empty list when the API returns no entries")
        void shouldReturnEmptyListWhenNoEntriesReturned() {
            // Arrange
            when(uniprotKbRestService.searchAll(anyString(), anyInt()))
                    .thenReturn(okResponse(List.of()));

            // Act
            var result = suggestion.suggest("ZZUNKNOWN");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when the API response body is null")
        void shouldReturnEmptyListWhenResponseBodyIsNull() {
            // Arrange
            when(uniprotKbRestService.searchAll(anyString(), anyInt()))
                    .thenReturn(ResponseEntity.ok(null));

            // Act
            var result = suggestion.suggest("receptor");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list and not rethrow when the API call throws")
        void shouldReturnEmptyListWhenApiThrowsRuntimeException() {
            // Arrange
            when(uniprotKbRestService.searchAll(anyString(), anyInt()))
                    .thenThrow(new RuntimeException("Simulated connection failure"));

            // Act — must not propagate the exception
            var result = suggestion.suggest("kinase");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list and not rethrow when the API call throws IllegalStateException")
        void shouldReturnEmptyListWhenApiThrowsIllegalStateException() {
            // Arrange
            when(uniprotKbRestService.searchAll(anyString(), anyInt()))
                    .thenThrow(new IllegalStateException("Unexpected API state"));

            // Act
            var result = suggestion.suggest("receptor");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should delegate suggest(field, query) to suggest(query) via default interface method")
        void shouldDelegateSuggestWithFieldToSuggestQuery() {
            // Arrange
            var entries = List.of(entryWithFullName("Cytochrome c"));
            when(uniprotKbRestService.searchAll(anyString(), anyInt())).thenReturn(okResponse(entries));

            // Act — exercises the default method from SuggestionService
            var result = suggestion.suggest("ProteinFullName", "cytochrome");

            // Assert
            assertThat(result).containsExactly("Cytochrome c");
        }
    }
}


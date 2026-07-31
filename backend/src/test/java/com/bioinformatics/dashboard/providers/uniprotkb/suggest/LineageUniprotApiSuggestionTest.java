package com.bioinformatics.dashboard.providers.uniprotkb.suggest;

import com.bioinformatics.dashboard.providers.uniprotkb.dto.Suggestion;
import com.bioinformatics.dashboard.providers.uniprotkb.dto.SuggestionResult;
import com.bioinformatics.dashboard.providers.uniprotkb.service.SuggesterRestService;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LineageUniprotApiSuggestion}.
 *
 * <p>Verifies correct taxonomy lineage value extraction from the UniProt suggester API,
 * deduplication, limit enforcement, and graceful error handling.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LineageUniprotApiSuggestion")
class LineageUniprotApiSuggestionTest {

    @Mock
    private SuggesterRestService suggesterRestService;

    @InjectMocks
    private LineageUniprotApiSuggestion suggestion;

    // ---- helpers ----------------------------------------------------------------

    private static ResponseEntity<SuggestionResult> responseOf(List<Suggestion> suggestions) {
        return ResponseEntity.ok(new SuggestionResult("test", "taxonomy", suggestions));
    }

    private static Suggestion taxon(String value) {
        return new Suggestion(value, String.valueOf(value.hashCode()));
    }

    // ---- tests ------------------------------------------------------------------

    @Nested
    @DisplayName("field()")
    class FieldMethod {

        @Test
        @DisplayName("should return 'Lineage' as the target field name")
        void shouldReturnLineageAsFieldName() {
            assertThat(suggestion.field()).isEqualTo("Lineage");
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
        @DisplayName("should return taxonomy lineage values from the API response")
        void shouldReturnLineageValuesFromApiResponse() {
            // Arrange
            var query = "Eukaryota";
            when(suggesterRestService.searchAll("taxonomy", query))
                    .thenReturn(responseOf(List.of(taxon("Eukaryota; Metazoa"), taxon("Eukaryota; Fungi"))));

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).containsExactly("Eukaryota; Metazoa", "Eukaryota; Fungi");
        }

        @Test
        @DisplayName("should always call the API with the 'taxonomy' dictionary key")
        void shouldCallApiWithTaxonomyDictionaryKey() {
            // Arrange
            var query = "Bacteria";
            when(suggesterRestService.searchAll("taxonomy", query))
                    .thenReturn(responseOf(List.of()));

            // Act
            suggestion.suggest(query);

            // Assert
            verify(suggesterRestService).searchAll("taxonomy", query);
            verifyNoMoreInteractions(suggesterRestService);
        }

        @Test
        @DisplayName("should deduplicate lineage values before enforcing the limit")
        void shouldDeduplicateLineageValues() {
            // Arrange — same value appears twice
            var query = "Archaea";
            when(suggesterRestService.searchAll("taxonomy", query))
                    .thenReturn(responseOf(List.of(
                            taxon("Archaea; Euryarchaeota"),
                            taxon("Archaea; Euryarchaeota"),
                            taxon("Archaea; Crenarchaeota"))));

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result)
                    .hasSize(2)
                    .containsExactlyInAnyOrder("Archaea; Euryarchaeota", "Archaea; Crenarchaeota");
        }

        @Test
        @DisplayName("should return at most 10 suggestions even when the API returns more")
        void shouldLimitResultsToTen() {
            // Arrange
            var query = "tax";
            var manySuggestions = IntStream.rangeClosed(1, 15)
                    .mapToObj(i -> taxon("Lineage" + i))
                    .toList();
            when(suggesterRestService.searchAll("taxonomy", query))
                    .thenReturn(responseOf(manySuggestions));

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).hasSize(10);
        }

        @Test
        @DisplayName("should return empty list when the API response has no body")
        void shouldReturnEmptyListWhenResponseHasNoBody() {
            // Arrange
            when(suggesterRestService.searchAll("taxonomy", "xyz"))
                    .thenReturn(ResponseEntity.noContent().build());

            // Act
            var result = suggestion.suggest("xyz");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when the API returns an empty suggestion list")
        void shouldReturnEmptyListWhenNoSuggestionsReturned() {
            // Arrange
            when(suggesterRestService.searchAll("taxonomy", "xyz"))
                    .thenReturn(responseOf(List.of()));

            // Act
            var result = suggestion.suggest("xyz");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list and not rethrow when the API call throws")
        void shouldReturnEmptyListWhenApiThrowsException() {
            // Arrange
            when(suggesterRestService.searchAll("taxonomy", "failing"))
                    .thenThrow(new RuntimeException("Simulated API failure"));

            // Act — must not propagate the exception
            var result = suggestion.suggest("failing");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should delegate suggest(field, query) to suggest(query) via default interface method")
        void shouldDelegateSuggestWithFieldToSuggestQuery() {
            // Arrange
            var query = "Metazoa";
            when(suggesterRestService.searchAll("taxonomy", query))
                    .thenReturn(responseOf(List.of(taxon("Eukaryota; Metazoa; Chordata"))));

            // Act — exercises the default method from SuggestionService
            var result = suggestion.suggest("Lineage", query);

            // Assert
            assertThat(result).containsExactly("Eukaryota; Metazoa; Chordata");
        }
    }
}


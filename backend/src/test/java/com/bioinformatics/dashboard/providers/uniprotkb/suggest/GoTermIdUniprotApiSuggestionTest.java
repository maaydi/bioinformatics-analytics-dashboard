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
 * Unit tests for {@link GoTermIdUniprotApiSuggestion}.
 *
 * <p>Verifies correct GO term identifier extraction, "GO:" prefix prepending,
 * deduplication, limit enforcement, and graceful error handling.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GoTermIdUniprotApiSuggestion")
class GoTermIdUniprotApiSuggestionTest {

    @Mock
    private SuggesterRestService suggesterRestService;

    @InjectMocks
    private GoTermIdUniprotApiSuggestion suggestion;

    // ---- helpers ----------------------------------------------------------------

    private static ResponseEntity<SuggestionResult> responseOf(List<Suggestion> suggestions) {
        return ResponseEntity.ok(new SuggestionResult("test", "go", suggestions));
    }

    private static Suggestion go(String rawId) {
        return new Suggestion("Some GO term label", rawId);
    }

    // ---- tests ------------------------------------------------------------------

    @Nested
    @DisplayName("field()")
    class FieldMethod {

        @Test
        @DisplayName("should return 'GoTermId' as the target field name")
        void shouldReturnGoTermIdAsFieldName() {
            assertThat(suggestion.field()).isEqualTo("GoTermId");
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
        @DisplayName("should prepend 'GO:' prefix to each suggestion id returned by the API")
        void shouldPrependGoPrefixToSuggestionIds() {
            // Arrange
            var query = "oxidase";
            when(suggesterRestService.searchAll("go", query))
                    .thenReturn(responseOf(List.of(go("0016491"), go("0004601"))));

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).containsExactly("GO:0016491", "GO:0004601");
        }

        @Test
        @DisplayName("should deduplicate GO term identifiers from the API response")
        void shouldDeduplicateGoTermIds() {
            // Arrange — same raw id appears twice
            var query = "kinase";
            when(suggesterRestService.searchAll("go", query))
                    .thenReturn(responseOf(List.of(go("0016301"), go("0016301"), go("0016740"))));

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result)
                    .hasSize(2)
                    .containsExactlyInAnyOrder("GO:0016301", "GO:0016740");
        }

        @Test
        @DisplayName("should return at most 10 suggestions even when the API returns more")
        void shouldLimitResultsToTen() {
            // Arrange
            var query = "go";
            var manySuggestions = IntStream.rangeClosed(1, 15)
                    .mapToObj(i -> go(String.format("%07d", i)))
                    .toList();
            when(suggesterRestService.searchAll("go", query))
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
            var query = "xyz";
            when(suggesterRestService.searchAll("go", query))
                    .thenReturn(ResponseEntity.noContent().build());

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when the API returns an empty suggestion list")
        void shouldReturnEmptyListWhenNoSuggestionsReturned() {
            // Arrange
            var query = "xyz";
            when(suggesterRestService.searchAll("go", query))
                    .thenReturn(responseOf(List.of()));

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty list and not rethrow when the API call throws")
        void shouldReturnEmptyListWhenApiThrowsException() {
            // Arrange
            var query = "failing";
            when(suggesterRestService.searchAll("go", query))
                    .thenThrow(new RuntimeException("Simulated API failure"));

            // Act — must not propagate the exception
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should always call the API with the 'go' dictionary key")
        void shouldCallApiWithGoDictionaryKey() {
            // Arrange
            var query = "transport";
            when(suggesterRestService.searchAll("go", query))
                    .thenReturn(responseOf(List.of()));

            // Act
            suggestion.suggest(query);

            // Assert — dictionary must always be "go", never another field
            verify(suggesterRestService).searchAll("go", query);
            verifyNoMoreInteractions(suggesterRestService);
        }

        @Test
        @DisplayName("should delegate suggest(field, query) to suggest(query) via default interface method")
        void shouldDelegateSuggestWithFieldToSuggestQuery() {
            // Arrange
            var query = "transport";
            when(suggesterRestService.searchAll("go", query))
                    .thenReturn(responseOf(List.of(go("0055085"))));

            // Act — exercises the default method from SuggestionService
            var result = suggestion.suggest("GoTermId", query);

            // Assert
            assertThat(result).containsExactly("GO:0055085");
        }
    }
}


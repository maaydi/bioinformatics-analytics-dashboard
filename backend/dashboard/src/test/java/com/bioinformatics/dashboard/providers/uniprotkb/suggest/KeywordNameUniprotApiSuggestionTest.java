package com.bioinformatics.dashboard.providers.uniprotkb.suggest;

import com.bioinformatics.common.providers.uniprotkb.dto.Suggestion;
import com.bioinformatics.common.providers.uniprotkb.dto.SuggestionResult;
import com.bioinformatics.common.providers.uniprotkb.service.SuggesterRestService;
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
 * Unit tests for {@link KeywordNameUniprotApiSuggestion}.
 *
 * <p>Verifies correct keyword name extraction from the UniProt suggester API,
 * deduplication, limit enforcement, and graceful error handling.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KeywordNameUniprotApiSuggestion")
class KeywordNameUniprotApiSuggestionTest {

    @Mock
    private SuggesterRestService suggesterRestService;

    @InjectMocks
    private KeywordNameUniprotApiSuggestion suggestion;

    // ---- helpers ----------------------------------------------------------------

    private static ResponseEntity<SuggestionResult> responseOf(List<Suggestion> suggestions) {
        return ResponseEntity.ok(new SuggestionResult("test", "keyword", suggestions));
    }

    private static Suggestion kw(String value) {
        return new Suggestion(value, "KW-" + value.hashCode());
    }

    // ---- tests ------------------------------------------------------------------

    @Nested
    @DisplayName("field()")
    class FieldMethod {

        @Test
        @DisplayName("should return 'KeywordName' as the target field name")
        void shouldReturnKeywordNameAsFieldName() {
            assertThat(suggestion.field()).isEqualTo("KeywordName");
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
        @DisplayName("should return keyword names from the API response")
        void shouldReturnKeywordNamesFromApiResponse() {
            // Arrange
            var query = "kinase";
            when(suggesterRestService.searchAll("keyword", query))
                    .thenReturn(responseOf(List.of(kw("Kinase"), kw("Protein kinase"))));

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).containsExactly("Kinase", "Protein kinase");
        }

        @Test
        @DisplayName("should always call the API with the 'keyword' dictionary key")
        void shouldCallApiWithKeywordDictionaryKey() {
            // Arrange
            var query = "membrane";
            when(suggesterRestService.searchAll("keyword", query))
                    .thenReturn(responseOf(List.of()));

            // Act
            suggestion.suggest(query);

            // Assert
            verify(suggesterRestService).searchAll("keyword", query);
            verifyNoMoreInteractions(suggesterRestService);
        }

        @Test
        @DisplayName("should deduplicate keyword names before enforcing the limit")
        void shouldDeduplicateKeywordNames() {
            // Arrange — same value appears twice
            var query = "transport";
            when(suggesterRestService.searchAll("keyword", query))
                    .thenReturn(responseOf(List.of(kw("Transport"), kw("Transport"), kw("Ion transport"))));

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result)
                    .hasSize(2)
                    .containsExactlyInAnyOrder("Transport", "Ion transport");
        }

        @Test
        @DisplayName("should return at most 10 suggestions even when the API returns more")
        void shouldLimitResultsToTen() {
            // Arrange
            var query = "kw";
            var manySuggestions = IntStream.rangeClosed(1, 15)
                    .mapToObj(i -> kw("Keyword" + i))
                    .toList();
            when(suggesterRestService.searchAll("keyword", query))
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
            when(suggesterRestService.searchAll("keyword", "xyz"))
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
            when(suggesterRestService.searchAll("keyword", "xyz"))
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
            when(suggesterRestService.searchAll("keyword", "failing"))
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
            var query = "receptor";
            when(suggesterRestService.searchAll("keyword", query))
                    .thenReturn(responseOf(List.of(kw("Receptor"))));

            // Act — exercises the default method from SuggestionService
            var result = suggestion.suggest("KeywordName", query);

            // Assert
            assertThat(result).containsExactly("Receptor");
        }
    }
}


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
 * Unit tests for {@link OrganismNameUniprotApiSuggestion}.
 *
 * <p>Verifies correct organism name extraction from the UniProt suggester API,
 * deduplication, limit enforcement, and graceful error handling.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrganismNameUniprotApiSuggestion")
class OrganismNameUniprotApiSuggestionTest {

    @Mock
    private SuggesterRestService suggesterRestService;

    @InjectMocks
    private OrganismNameUniprotApiSuggestion suggestion;

    // ---- helpers ----------------------------------------------------------------

    private static ResponseEntity<SuggestionResult> responseOf(List<Suggestion> suggestions) {
        return ResponseEntity.ok(new SuggestionResult("test", "organism", suggestions));
    }

    private static Suggestion organism(String value) {
        return new Suggestion(value, String.valueOf(value.hashCode()));
    }

    // ---- tests ------------------------------------------------------------------

    @Nested
    @DisplayName("field()")
    class FieldMethod {

        @Test
        @DisplayName("should return 'OrganismName' as the target field name")
        void shouldReturnOrganismNameAsFieldName() {
            assertThat(suggestion.field()).isEqualTo("OrganismName");
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
        @DisplayName("should return organism names from the API response")
        void shouldReturnOrganismNamesFromApiResponse() {
            // Arrange
            var query = "Homo";
            when(suggesterRestService.searchAll("organism", query))
                    .thenReturn(responseOf(List.of(organism("Homo sapiens"), organism("Homo heidelbergensis"))));

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).containsExactly("Homo sapiens", "Homo heidelbergensis");
        }

        @Test
        @DisplayName("should always call the API with the 'organism' dictionary key")
        void shouldCallApiWithOrganismDictionaryKey() {
            // Arrange
            var query = "Mus";
            when(suggesterRestService.searchAll("organism", query))
                    .thenReturn(responseOf(List.of()));

            // Act
            suggestion.suggest(query);

            // Assert
            verify(suggesterRestService).searchAll("organism", query);
            verifyNoMoreInteractions(suggesterRestService);
        }

        @Test
        @DisplayName("should deduplicate organism names before enforcing the limit")
        void shouldDeduplicateOrganismNames() {
            // Arrange — same value appears twice
            var query = "Rattus";
            when(suggesterRestService.searchAll("organism", query))
                    .thenReturn(responseOf(List.of(
                            organism("Rattus norvegicus"),
                            organism("Rattus norvegicus"),
                            organism("Rattus rattus"))));

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result)
                    .hasSize(2)
                    .containsExactlyInAnyOrder("Rattus norvegicus", "Rattus rattus");
        }

        @Test
        @DisplayName("should return at most 10 suggestions even when the API returns more")
        void shouldLimitResultsToTen() {
            // Arrange
            var query = "org";
            var manySuggestions = IntStream.rangeClosed(1, 15)
                    .mapToObj(i -> organism("Organism" + i))
                    .toList();
            when(suggesterRestService.searchAll("organism", query))
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
            when(suggesterRestService.searchAll("organism", "xyz"))
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
            when(suggesterRestService.searchAll("organism", "xyz"))
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
            when(suggesterRestService.searchAll("organism", "failing"))
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
            var query = "Arabidopsis";
            when(suggesterRestService.searchAll("organism", query))
                    .thenReturn(responseOf(List.of(organism("Arabidopsis thaliana"))));

            // Act — exercises the default method from SuggestionService
            var result = suggestion.suggest("OrganismName", query);

            // Assert
            assertThat(result).containsExactly("Arabidopsis thaliana");
        }
    }
}


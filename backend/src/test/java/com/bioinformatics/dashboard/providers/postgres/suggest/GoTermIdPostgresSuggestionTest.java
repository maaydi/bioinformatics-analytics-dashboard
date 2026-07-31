package com.bioinformatics.dashboard.providers.postgres.suggest;

import com.bioinformatics.dashboard.providers.postgres.gene.repository.GoTermRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GoTermIdPostgresSuggestion}.
 *
 * <p>Verifies that the suggestion provider correctly delegates to the repository
 * and propagates results without transformation.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GoTermIdPostgresSuggestion")
class GoTermIdPostgresSuggestionTest {

    @Mock
    private GoTermRepository goTermRepository;

    @InjectMocks
    private GoTermIdPostgresSuggestion suggestion;

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
        @DisplayName("should return 'postgres' as the provider name")
        void shouldReturnPostgresAsProviderName() {
            assertThat(suggestion.getProviderName()).isEqualTo("postgres");
        }
    }

    @Nested
    @DisplayName("suggest(String query)")
    class SuggestMethod {

        @Test
        @DisplayName("should return suggestions from the repository when matches are found")
        void shouldReturnSuggestionsWhenRepositoryReturnsResults() {
            // Arrange
            var query = "GO:000";
            var expected = List.of("GO:0001234", "GO:0005678", "GO:0009012");
            when(goTermRepository.findTop10ByGoIdContainingIgnoreCase(query)).thenReturn(expected);

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).containsExactlyElementsOf(expected);
            verify(goTermRepository).findTop10ByGoIdContainingIgnoreCase(query);
        }

        @Test
        @DisplayName("should return empty list when no matches are found")
        void shouldReturnEmptyListWhenRepositoryReturnsNoResults() {
            // Arrange
            var query = "NONEXISTENT";
            when(goTermRepository.findTop10ByGoIdContainingIgnoreCase(query)).thenReturn(List.of());

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).isEmpty();
            verify(goTermRepository).findTop10ByGoIdContainingIgnoreCase(query);
        }

        @Test
        @DisplayName("should return at most 10 results (repository enforces LIMIT 10)")
        void shouldReturnAtMostTenResults() {
            // Arrange
            var query = "GO:";
            var repositoryResults = List.of(
                    "GO:0000001", "GO:0000002", "GO:0000003", "GO:0000004", "GO:0000005",
                    "GO:0000006", "GO:0000007", "GO:0000008", "GO:0000009", "GO:0000010"
            );
            when(goTermRepository.findTop10ByGoIdContainingIgnoreCase(query)).thenReturn(repositoryResults);

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).hasSize(10);
        }

        @Test
        @DisplayName("should forward query verbatim to the repository without transformation")
        void shouldPassQueryAsIsToRepository() {
            // Arrange
            var query = "  go:0007  ";
            when(goTermRepository.findTop10ByGoIdContainingIgnoreCase(query)).thenReturn(List.of());

            // Act
            suggestion.suggest(query);

            // Assert
            verify(goTermRepository).findTop10ByGoIdContainingIgnoreCase(query);
            verifyNoMoreInteractions(goTermRepository);
        }

        @Test
        @DisplayName("should delegate suggest(field, query) to suggest(query) via default interface method")
        void shouldDelegateSuggestWithFieldToSuggestQuery() {
            // Arrange
            var query = "oxidase";
            var expected = List.of("GO:0016491");
            when(goTermRepository.findTop10ByGoIdContainingIgnoreCase(query)).thenReturn(expected);

            // Act — uses the default method from SuggestionService
            var result = suggestion.suggest("GoTermId", query);

            // Assert
            assertThat(result).isEqualTo(expected);
        }
    }
}


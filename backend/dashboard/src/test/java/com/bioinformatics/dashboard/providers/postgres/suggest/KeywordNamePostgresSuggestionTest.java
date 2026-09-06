package com.bioinformatics.dashboard.providers.postgres.suggest;

import com.bioinformatics.common.gene.repository.KeywordRepository;
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
 * Unit tests for {@link KeywordNamePostgresSuggestion}.
 *
 * <p>Verifies correct delegation to the repository and result propagation without transformation.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KeywordNamePostgresSuggestion")
class KeywordNamePostgresSuggestionTest {

    @Mock
    private KeywordRepository repository;

    @InjectMocks
    private KeywordNamePostgresSuggestion suggestion;

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
        @DisplayName("should return 'postgres' as the provider name")
        void shouldReturnPostgresAsProviderName() {
            assertThat(suggestion.getProviderName()).isEqualTo("postgres");
        }
    }

    @Nested
    @DisplayName("suggest(String query)")
    class SuggestMethod {

        @Test
        @DisplayName("should return keyword names from the repository when matches are found")
        void shouldReturnResultsWhenRepositoryReturnsMatches() {
            // Arrange
            var query = "kinase";
            var expected = List.of("Kinase", "Protein kinase", "Serine/threonine-protein kinase");
            when(repository.findTop10ByNameContainingIgnoreCase(query)).thenReturn(expected);

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).containsExactlyElementsOf(expected);
            verify(repository).findTop10ByNameContainingIgnoreCase(query);
        }

        @Test
        @DisplayName("should return empty list when no keyword matches are found")
        void shouldReturnEmptyListWhenRepositoryReturnsNoResults() {
            // Arrange
            var query = "ZZZUNKNOWNKEYWORD";
            when(repository.findTop10ByNameContainingIgnoreCase(query)).thenReturn(List.of());

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).isEmpty();
            verify(repository).findTop10ByNameContainingIgnoreCase(query);
        }

        @Test
        @DisplayName("should return at most 10 results (repository enforces LIMIT 10)")
        void shouldReturnAtMostTenResults() {
            // Arrange
            var query = "protein";
            var repositoryResults = List.of(
                    "Protein kinase", "Protein phosphatase", "Protein transport",
                    "Protein biosynthesis", "Protein degradation", "Protein folding",
                    "Protein repair", "Protein splicing", "Protein export", "Protein import"
            );
            when(repository.findTop10ByNameContainingIgnoreCase(query)).thenReturn(repositoryResults);

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).hasSize(10);
        }

        @Test
        @DisplayName("should forward the query verbatim to the repository without transformation")
        void shouldPassQueryAsIsToRepository() {
            // Arrange
            var query = "  Membrane  ";
            when(repository.findTop10ByNameContainingIgnoreCase(query)).thenReturn(List.of());

            // Act
            suggestion.suggest(query);

            // Assert
            verify(repository).findTop10ByNameContainingIgnoreCase(query);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("should handle empty query by passing it to the repository")
        void shouldHandleEmptyQuery() {
            // Arrange
            var expected = List.of("ATP-binding", "ATPase");
            when(repository.findTop10ByNameContainingIgnoreCase("")).thenReturn(expected);

            // Act
            var result = suggestion.suggest("");

            // Assert
            assertThat(result).containsExactlyElementsOf(expected);
            verify(repository).findTop10ByNameContainingIgnoreCase("");
        }

        @Test
        @DisplayName("should delegate suggest(field, query) to suggest(query) via default interface method")
        void shouldDelegateSuggestWithFieldToSuggestQuery() {
            // Arrange
            var query = "receptor";
            var expected = List.of("Receptor");
            when(repository.findTop10ByNameContainingIgnoreCase(query)).thenReturn(expected);

            // Act — exercises the default method from SuggestionService
            var result = suggestion.suggest("KeywordName", query);

            // Assert
            assertThat(result).isEqualTo(expected);
        }
    }
}


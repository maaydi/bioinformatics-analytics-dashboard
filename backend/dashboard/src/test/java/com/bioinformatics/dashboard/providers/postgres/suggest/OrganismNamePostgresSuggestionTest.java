package com.bioinformatics.dashboard.providers.postgres.suggest;

import com.bioinformatics.dashboard.providers.postgres.gene.repository.ProteinEntryRepository;
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
 * Unit tests for {@link OrganismNamePostgresSuggestion}.
 *
 * <p>Verifies correct delegation to the repository and result propagation without transformation.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrganismNamePostgresSuggestion")
class OrganismNamePostgresSuggestionTest {

    @Mock
    private ProteinEntryRepository repository;

    @InjectMocks
    private OrganismNamePostgresSuggestion suggestion;

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
        @DisplayName("should return 'postgres' as the provider name")
        void shouldReturnPostgresAsProviderName() {
            assertThat(suggestion.getProviderName()).isEqualTo("postgres");
        }
    }

    @Nested
    @DisplayName("suggest(String query)")
    class SuggestMethod {

        @Test
        @DisplayName("should return organism names from the repository when matches are found")
        void shouldReturnResultsWhenRepositoryReturnsMatches() {
            // Arrange
            var query = "Homo";
            var expected = List.of("Homo sapiens", "Homo heidelbergensis");
            when(repository.findTop10ByOrganismNameContainingIgnoreCase(query)).thenReturn(expected);

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).containsExactlyElementsOf(expected);
            verify(repository).findTop10ByOrganismNameContainingIgnoreCase(query);
        }

        @Test
        @DisplayName("should return empty list when no organism name matches are found")
        void shouldReturnEmptyListWhenRepositoryReturnsNoResults() {
            // Arrange
            var query = "ZZUNKNOWNORGANISM";
            when(repository.findTop10ByOrganismNameContainingIgnoreCase(query)).thenReturn(List.of());

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).isEmpty();
            verify(repository).findTop10ByOrganismNameContainingIgnoreCase(query);
        }

        @Test
        @DisplayName("should return at most 10 results (repository enforces LIMIT 10)")
        void shouldReturnAtMostTenResults() {
            // Arrange
            var query = "Mus";
            var repositoryResults = List.of(
                    "Mus musculus", "Mus spretus", "Mus caroli",
                    "Mus cookii", "Mus macedonicus", "Mus minutoides",
                    "Mus pahari", "Mus platythrix", "Mus setulosus", "Mus shortridgei"
            );
            when(repository.findTop10ByOrganismNameContainingIgnoreCase(query)).thenReturn(repositoryResults);

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).hasSize(10);
        }

        @Test
        @DisplayName("should forward the query verbatim to the repository without transformation")
        void shouldPassQueryAsIsToRepository() {
            // Arrange
            var query = "  Arabidopsis  ";
            when(repository.findTop10ByOrganismNameContainingIgnoreCase(query)).thenReturn(List.of());

            // Act
            suggestion.suggest(query);

            // Assert
            verify(repository).findTop10ByOrganismNameContainingIgnoreCase(query);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("should handle empty query by passing it to the repository")
        void shouldHandleEmptyQuery() {
            // Arrange
            var expected = List.of("Escherichia coli", "Saccharomyces cerevisiae");
            when(repository.findTop10ByOrganismNameContainingIgnoreCase("")).thenReturn(expected);

            // Act
            var result = suggestion.suggest("");

            // Assert
            assertThat(result).containsExactlyElementsOf(expected);
            verify(repository).findTop10ByOrganismNameContainingIgnoreCase("");
        }

        @Test
        @DisplayName("should delegate suggest(field, query) to suggest(query) via default interface method")
        void shouldDelegateSuggestWithFieldToSuggestQuery() {
            // Arrange
            var query = "Rattus";
            var expected = List.of("Rattus norvegicus");
            when(repository.findTop10ByOrganismNameContainingIgnoreCase(query)).thenReturn(expected);

            // Act — exercises the default method from SuggestionService
            var result = suggestion.suggest("OrganismName", query);

            // Assert
            assertThat(result).isEqualTo(expected);
        }
    }
}


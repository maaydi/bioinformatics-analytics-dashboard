package com.bioinformatics.dashboard.providers.postgres.suggest;

import com.bioinformatics.common.gene.repository.ProteinEntryRepository;
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
 * Unit tests for {@link ProteinFullNamePostgresSuggestion}.
 *
 * <p>Verifies correct delegation to the repository and result propagation without transformation.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProteinFullNamePostgresSuggestion")
class ProteinFullNamePostgresSuggestionTest {

    @Mock
    private ProteinEntryRepository repository;

    @InjectMocks
    private ProteinFullNamePostgresSuggestion suggestion;

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
        @DisplayName("should return 'postgres' as the provider name")
        void shouldReturnPostgresAsProviderName() {
            assertThat(suggestion.getProviderName()).isEqualTo("postgres");
        }
    }

    @Nested
    @DisplayName("suggest(String query)")
    class SuggestMethod {

        @Test
        @DisplayName("should return protein full names from the repository when matches are found")
        void shouldReturnResultsWhenRepositoryReturnsMatches() {
            // Arrange
            var query = "Tumor";
            var expected = List.of(
                    "Tumor protein p53",
                    "Tumor necrosis factor receptor superfamily member 1A"
            );
            when(repository.findTop10ByProteinFullNameContainingIgnoreCase(query)).thenReturn(expected);

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).containsExactlyElementsOf(expected);
            verify(repository).findTop10ByProteinFullNameContainingIgnoreCase(query);
        }

        @Test
        @DisplayName("should return empty list when no protein full name matches are found")
        void shouldReturnEmptyListWhenRepositoryReturnsNoResults() {
            // Arrange
            var query = "ZZUNKNOWNPROTEIN";
            when(repository.findTop10ByProteinFullNameContainingIgnoreCase(query)).thenReturn(List.of());

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).isEmpty();
            verify(repository).findTop10ByProteinFullNameContainingIgnoreCase(query);
        }

        @Test
        @DisplayName("should return at most 10 results (repository enforces LIMIT 10)")
        void shouldReturnAtMostTenResults() {
            // Arrange
            var query = "kinase";
            var repositoryResults = List.of(
                    "Tyrosine-protein kinase ABL1",
                    "Serine/threonine-protein kinase ATM",
                    "Serine/threonine-protein kinase ATR",
                    "Serine/threonine-protein kinase CHEK1",
                    "Serine/threonine-protein kinase CHEK2",
                    "Tyrosine-protein kinase SRC",
                    "Tyrosine-protein kinase YES",
                    "Serine/threonine-protein kinase PLK1",
                    "Serine/threonine-protein kinase Aurora-A",
                    "Serine/threonine-protein kinase Aurora-B"
            );
            when(repository.findTop10ByProteinFullNameContainingIgnoreCase(query)).thenReturn(repositoryResults);

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).hasSize(10);
        }

        @Test
        @DisplayName("should forward the query verbatim to the repository without transformation")
        void shouldPassQueryAsIsToRepository() {
            // Arrange
            var query = "  Epidermal growth factor  ";
            when(repository.findTop10ByProteinFullNameContainingIgnoreCase(query)).thenReturn(List.of());

            // Act
            suggestion.suggest(query);

            // Assert
            verify(repository).findTop10ByProteinFullNameContainingIgnoreCase(query);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("should handle empty query by passing it to the repository")
        void shouldHandleEmptyQuery() {
            // Arrange
            var expected = List.of("Cytochrome c", "Hemoglobin subunit alpha");
            when(repository.findTop10ByProteinFullNameContainingIgnoreCase("")).thenReturn(expected);

            // Act
            var result = suggestion.suggest("");

            // Assert
            assertThat(result).containsExactlyElementsOf(expected);
            verify(repository).findTop10ByProteinFullNameContainingIgnoreCase("");
        }

        @Test
        @DisplayName("should delegate suggest(field, query) to suggest(query) via default interface method")
        void shouldDelegateSuggestWithFieldToSuggestQuery() {
            // Arrange
            var query = "receptor";
            var expected = List.of("Epidermal growth factor receptor");
            when(repository.findTop10ByProteinFullNameContainingIgnoreCase(query)).thenReturn(expected);

            // Act — exercises the default method from SuggestionService
            var result = suggestion.suggest("ProteinFullName", query);

            // Assert
            assertThat(result).isEqualTo(expected);
        }
    }
}


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
 * Unit tests for {@link LineagePostgresSuggestion}.
 *
 * <p>Verifies correct delegation to the repository and result propagation without transformation.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LineagePostgresSuggestion")
class LineagePostgresSuggestionTest {

    @Mock
    private ProteinEntryRepository repository;

    @InjectMocks
    private LineagePostgresSuggestion suggestion;

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
        @DisplayName("should return 'postgres' as the provider name")
        void shouldReturnPostgresAsProviderName() {
            assertThat(suggestion.getProviderName()).isEqualTo("postgres");
        }
    }

    @Nested
    @DisplayName("suggest(String query)")
    class SuggestMethod {

        @Test
        @DisplayName("should return lineage values from the repository when matches are found")
        void shouldReturnResultsWhenRepositoryReturnsMatches() {
            // Arrange
            var query = "Eukaryota";
            var expected = List.of("Eukaryota; Metazoa; Chordata", "Eukaryota; Fungi");
            when(repository.findTop10ByLineageContainingIgnoreCase(query)).thenReturn(expected);

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).containsExactlyElementsOf(expected);
            verify(repository).findTop10ByLineageContainingIgnoreCase(query);
        }

        @Test
        @DisplayName("should return empty list when no lineage matches are found")
        void shouldReturnEmptyListWhenRepositoryReturnsNoResults() {
            // Arrange
            var query = "ZZUNKNOWNLINEAGE";
            when(repository.findTop10ByLineageContainingIgnoreCase(query)).thenReturn(List.of());

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).isEmpty();
            verify(repository).findTop10ByLineageContainingIgnoreCase(query);
        }

        @Test
        @DisplayName("should return at most 10 results (repository enforces LIMIT 10)")
        void shouldReturnAtMostTenResults() {
            // Arrange
            var query = "Bacteria";
            var repositoryResults = List.of(
                    "Bacteria; Firmicutes", "Bacteria; Proteobacteria",
                    "Bacteria; Actinobacteria", "Bacteria; Bacteroidetes",
                    "Bacteria; Cyanobacteria", "Bacteria; Spirochaetes",
                    "Bacteria; Tenericutes", "Bacteria; Verrucomicrobia",
                    "Bacteria; Chlamydiae", "Bacteria; Fusobacteria"
            );
            when(repository.findTop10ByLineageContainingIgnoreCase(query)).thenReturn(repositoryResults);

            // Act
            var result = suggestion.suggest(query);

            // Assert
            assertThat(result).hasSize(10);
        }

        @Test
        @DisplayName("should forward the query verbatim to the repository without transformation")
        void shouldPassQueryAsIsToRepository() {
            // Arrange
            var query = "  Archaea  ";
            when(repository.findTop10ByLineageContainingIgnoreCase(query)).thenReturn(List.of());

            // Act
            suggestion.suggest(query);

            // Assert
            verify(repository).findTop10ByLineageContainingIgnoreCase(query);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("should handle empty query by passing it to the repository")
        void shouldHandleEmptyQuery() {
            // Arrange
            var expected = List.of("Viruses; Retroviridae", "Viruses; Adenoviridae");
            when(repository.findTop10ByLineageContainingIgnoreCase("")).thenReturn(expected);

            // Act
            var result = suggestion.suggest("");

            // Assert
            assertThat(result).containsExactlyElementsOf(expected);
            verify(repository).findTop10ByLineageContainingIgnoreCase("");
        }

        @Test
        @DisplayName("should delegate suggest(field, query) to suggest(query) via default interface method")
        void shouldDelegateSuggestWithFieldToSuggestQuery() {
            // Arrange
            var query = "Metazoa";
            var expected = List.of("Eukaryota; Metazoa; Chordata");
            when(repository.findTop10ByLineageContainingIgnoreCase(query)).thenReturn(expected);

            // Act — exercises the default method from SuggestionService
            var result = suggestion.suggest("Lineage", query);

            // Assert
            assertThat(result).isEqualTo(expected);
        }
    }
}


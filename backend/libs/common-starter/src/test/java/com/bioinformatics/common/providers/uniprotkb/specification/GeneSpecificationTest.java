package com.bioinformatics.common.providers.uniprotkb.specification;

import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.common.providers.uniprotkb.dto.Suggestion;
import com.bioinformatics.common.providers.uniprotkb.dto.SuggestionResult;
import com.bioinformatics.common.providers.uniprotkb.gene.specification.GeneSpecification;
import com.bioinformatics.common.providers.uniprotkb.service.UniProtSearchFieldService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeneSpecification")
class GeneSpecificationTest {

    @Mock
    private UniProtSearchFieldService uniProtSearchFieldService;

    @InjectMocks
    private GeneSpecification specification;

    private static ResponseEntity<SuggestionResult> suggestionResultWithIds(String... ids) {
        var suggestions = java.util.Arrays.stream(ids)
                .map(id -> new Suggestion("value-" + id, id))
                .toList();
        return ResponseEntity.ok(new SuggestionResult("query", "keyword", suggestions));
    }

    @Nested
    @DisplayName("fromRequest")
    class FromRequest {

        @Test
        void returnsMatchAllForNullRequest() {
            assertThat(specification.fromRequest(null)).isEqualTo("(*)");
        }

        @Test
        void returnsMatchAllForEmptyRequest() {
            assertThat(specification.fromRequest(GeneSearchRequest.builder().build())).isEqualTo("(*)");
        }

        @Test
        void wrapsSingleClauseAndOuterQuery() {
            var request = GeneSearchRequest.builder().accession("p12345").build();

            assertThat(specification.fromRequest(request)).isEqualTo("((accession:P12345))");
        }

        @Test
        void joinsActiveClausesWithAndInsideOuterParentheses() {
            var request = GeneSearchRequest.builder()
                    .accession("P12345")
                    .reviewed(true)
                    .goTermId("GO:0006915")
                    .build();

            assertThat(specification.fromRequest(request))
                    .isEqualTo("((accession:P12345) AND (reviewed:true) AND (go:0006915))");
        }

        @Test
        void includesMappedKeywordIdsAndMappedFeatureTypeFields() {
            given(uniProtSearchFieldService.getCachedFeatureTypes())
                    .willReturn(Map.of("Feature group", List.of("ft_chain", "ft_domain")));

            var request = GeneSearchRequest.builder()
                    .keywords(List.of("Kinase"))
                    .featureType("Feature group")
                    .build();

            assertThat(specification.fromRequest(request))
                    .isEqualTo("((keyword:Kinase) AND (ft_chain:* OR ft_domain:*))");
        }
    }

    @Nested
    @DisplayName("globalSearch")
    class GlobalSearch {

        @Test
        void returnsEmptyWhenInputIsNullOrBlank() {
            assertThat(specification.globalSearch(null)).isEmpty();
            assertThat(specification.globalSearch("  ")).isEmpty();
        }

        @Test
        void escapesSpecialCharactersAndAppendsWildcard() {
            var result = specification.globalSearch("  p53+alpha&&beta||go:1  ");

            assertThat(result).hasValue("(p53\\+alpha\\&&beta\\||go\\:1*)");
        }
    }

    @Nested
    @DisplayName("goTermId")
    class GoTermId {

        @Test
        void stripsGoPrefixWhenPresent() {
            assertThat(specification.goTermId("GO:0006915")).hasValue("(go:0006915)");
        }

        @Test
        void keepsIdWhenNoPrefix() {
            assertThat(specification.goTermId("0006915")).hasValue("(go:0006915)");
        }
    }

    @Nested
    @DisplayName("featureType")
    class FeatureType {

        @Test
        void returnsEmptyWhenInputIsNullOrBlank() {
            assertThat(specification.featureType(null)).isEmpty();
            assertThat(specification.featureType("   ")).isEmpty();
            verifyNoInteractions(uniProtSearchFieldService);
        }

        @Test
        void returnsEmptyWhenTypeIsNotMapped() {
            given(uniProtSearchFieldService.getCachedFeatureTypes())
                    .willReturn(Map.of("Known", List.of("ft_chain")));

            assertThat(specification.featureType("Unknown")).isEmpty();
        }

        @Test
        void mapsFeatureTypeToOrClauseWithWildcards() {
            given(uniProtSearchFieldService.getCachedFeatureTypes())
                    .willReturn(Map.of("Known", List.of("ft_chain", "ft_domain")));

            assertThat(specification.featureType("Known"))
                    .hasValue("(ft_chain:* OR ft_domain:*)");
        }
    }

    @Nested
    @DisplayName("keywords")
    class Keywords {

        @Test
        void returnsEmptyWhenInputIsNullOrEmpty() {
            assertThat(specification.keywords(null)).isEmpty();
            assertThat(specification.keywords(List.of())).isEmpty();
        }

    }
}


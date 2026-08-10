package com.bioinformatics.dashboard.providers.uniprotkb.suggest;

import com.bioinformatics.dashboard.providers.uniprotkb.service.UniProtSearchFieldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FeatureTypeUniprotApiSuggestion}.
 *
 * <p>The SUT filters keys of the map returned by
 * {@link UniProtSearchFieldService#getCachedFeatureTypes()} using a
 * case-insensitive contains check, then limits the result to 10 entries.
 *
 * <p>Unlike the Accession variant (which generates patterns), this provider
 * queries a real cached service — so {@link UniProtSearchFieldService} is mocked.
 */
@ExtendWith(MockitoExtension.class)
class FeatureTypeUniprotApiSuggestionTest {
    @Mock
    private UniProtSearchFieldService service;
    private FeatureTypeUniprotApiSuggestion suggestion;

    @BeforeEach
    void setUp() {
        suggestion = new FeatureTypeUniprotApiSuggestion(service);
    }

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------
    @Test
    void field_returnsFeatureType() {
        assertThat(suggestion.field()).isEqualTo("FeatureType");
    }

    @Test
    void getProviderName_returnsUniprotKb() {
        assertThat(suggestion.getProviderName()).isEqualTo("uniprotKb");
    }

    // -------------------------------------------------------------------------
    // suggest(String) — happy-path filtering
    // -------------------------------------------------------------------------
    @Test
    void suggest_returnsKeysContainingQueryIgnoreCase() {
        var featureTypes = Map.of(
                "Active site", List.of("ft_act_site"),
                "Binding site", List.of("ft_binding"),
                "Chain", List.of("ft_chain"),
                "Coiled coil", List.of("ft_coiled")
        );
        when(service.getCachedFeatureTypes()).thenReturn(featureTypes);
        var result = suggestion.suggest("site");
        assertThat(result)
                .hasSize(2)
                .containsExactlyInAnyOrder("Active site", "Binding site");
    }

    @ParameterizedTest
    @ValueSource(strings = {"site", "SITE", "Site", "SiTe"})
    void suggest_isCaseInsensitive(final String query) {
        var featureTypes = Map.of(
                "Active site", List.of("ft_act_site"),
                "Binding site", List.of("ft_binding"),
                "Chain", List.of("ft_chain")
        );
        when(service.getCachedFeatureTypes()).thenReturn(featureTypes);
        var result = suggestion.suggest(query);
        assertThat(result).hasSize(2);
    }

    @Test
    void suggest_delegatesToService() {
        when(service.getCachedFeatureTypes()).thenReturn(Map.of("Chain", List.of("ft_chain")));
        suggestion.suggest("chain");
        verify(service).getCachedFeatureTypes();
    }

    // -------------------------------------------------------------------------
    // suggest(String) — limit enforcement
    // -------------------------------------------------------------------------
    @Test
    void suggest_limitsResultsToTen() {
        Map<String, List<String>> featureTypes = new HashMap<>();
        IntStream.rangeClosed(1, 15).forEach(i -> featureTypes.put("Feature " + i, List.of("ft_" + i)));
        when(service.getCachedFeatureTypes()).thenReturn(featureTypes);
        var result = suggestion.suggest("feature");
        assertThat(result).hasSize(10);
    }

    // -------------------------------------------------------------------------
    // suggest(String) — null key filtering
    // -------------------------------------------------------------------------
    @Test
    void suggest_filtersOutNullKeys() {
        // getCachedFeatureTypes() may return a map with a null key in edge cases
        Map<String, List<String>> featureTypesWithNull = new HashMap<>();
        featureTypesWithNull.put(null, List.of("ft_unknown"));
        featureTypesWithNull.put("Active site", List.of("ft_act_site"));
        when(service.getCachedFeatureTypes()).thenReturn(featureTypesWithNull);
        var result = suggestion.suggest("site");
        assertThat(result)
                .doesNotContainNull()
                .containsExactly("Active site");
    }

    // -------------------------------------------------------------------------
    // suggest(String) — empty / no-match scenarios
    // -------------------------------------------------------------------------
    @Test
    void suggest_emptyMap_returnsEmptyList() {
        when(service.getCachedFeatureTypes()).thenReturn(Map.of());
        var result = suggestion.suggest("chain");
        assertThat(result).isEmpty();
    }

    @Test
    void suggest_noKeyMatchesQuery_returnsEmptyList() {
        when(service.getCachedFeatureTypes()).thenReturn(Map.of("Chain", List.of("ft_chain")));
        var result = suggestion.suggest("XXXXXX");
        assertThat(result).isEmpty();
    }

    @Test
    void suggest_emptyQuery_returnsAllMatchingKeys_upToTen() {
        // Every key contains "" so all 12 entries match, but the limit of 10 must be respected
        Map<String, List<String>> featureTypes = new HashMap<>();
        IntStream.rangeClosed(1, 12).forEach(i -> featureTypes.put("Type " + i, List.of("ft_" + i)));
        when(service.getCachedFeatureTypes()).thenReturn(featureTypes);
        var result = suggestion.suggest("");
        assertThat(result).hasSize(10);
    }

    // -------------------------------------------------------------------------
    // suggest(String field, String query) — default interface method delegation
    // -------------------------------------------------------------------------
    @Test
    void suggest_withFieldParam_delegatesToSuggestQuery() {
        var featureTypes = Map.of("Binding site", List.of("ft_binding"));
        when(service.getCachedFeatureTypes()).thenReturn(featureTypes);
        var result = suggestion.suggest("FeatureType", "binding");
        assertThat(result).containsExactly("Binding site");
    }
}

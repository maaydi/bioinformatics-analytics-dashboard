package com.bioinformatics.dashboard.providers.uniprotkb.suggest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AccessionUniprotApiSuggestion}.
 *
 * <p>The tested regex is:
 * {@code (?i)([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?}
 *
 * <p>Because {@link AccessionUniprotApiSuggestion#suggest} is non-deterministic,
 * tests assert invariants (format, count, containment) rather than exact values.
 */
class AccessionUniprotApiSuggestionTest {

    /**
     * Same regex the SUT uses to validate generated accessions.
     */
    private static final Pattern ACCESSION_PATTERN = Pattern.compile(
            "^(?i)([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?$"
    );

    private AccessionUniprotApiSuggestion suggestion;

    @BeforeEach
    void setUp() {
        suggestion = new AccessionUniprotApiSuggestion();
    }

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    @Test
    void field_returnsAccession() {
        assertThat(suggestion.field()).isEqualTo("Accession");
    }

    @Test
    void getProviderName_returnsUniprotKb() {
        assertThat(suggestion.getProviderName()).isEqualTo("uniprotKb");
    }

    // -------------------------------------------------------------------------
    // Output invariants — valid UniProt accession format
    // -------------------------------------------------------------------------

    @Test
    void suggest_withValidPrefix_returnsUpToTenResults() {
        var results = suggestion.suggest("P2");

        assertThat(results)
                .isNotNull()
                .hasSizeLessThanOrEqualTo(10);
    }

    @RepeatedTest(5)
    void suggest_allResultsMatchAccessionPattern() {
        var results = suggestion.suggest("O1");

        assertThat(results).allSatisfy(accession ->
                assertThat(ACCESSION_PATTERN.matcher(accession).matches())
                        .as("Expected '%s' to match the UniProt accession pattern", accession)
                        .isTrue()
        );
    }

    @RepeatedTest(5)
    void suggest_resultsAreUnique() {
        var results = suggestion.suggest("A0");

        assertThat(results).doesNotHaveDuplicates();
    }

    @ParameterizedTest
    @ValueSource(strings = {"P2", "O1", "Q9", "A0"})
    void suggest_eachResultContainsInputSubstring(String query) {
        var upperQuery = query.toUpperCase();
        var results = suggestion.suggest(query);

        assertThat(results).allSatisfy(accession ->
                assertThat(accession.toUpperCase()).contains(upperQuery)
        );
    }

    // -------------------------------------------------------------------------
    // Case insensitivity
    // -------------------------------------------------------------------------

    @Test
    void suggest_inputNormalizedToUppercase_stillProducesResults() {
        var lower = suggestion.suggest("p2");
        var upper = suggestion.suggest("P2");

        // Both calls must produce results (exact values may differ — non-deterministic)
        assertThat(lower).isNotEmpty();
        assertThat(upper).isNotEmpty();
    }

    // -------------------------------------------------------------------------
    // Full valid accession as input — must still generate completions
    // -------------------------------------------------------------------------

    @Test
    void suggest_fullSixCharAccession_returnsResults() {
        // P21802 is a valid Swiss-Prot accession — exactly fills template 0
        var results = suggestion.suggest("P21802");

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(acc -> ACCESSION_PATTERN.matcher(acc).matches());
    }

    // -------------------------------------------------------------------------
    // Invalid / impossible input — must return empty list, not throw
    // -------------------------------------------------------------------------

    @Test
    void suggest_inputThatCannotFitAnyTemplate_returnsEmptyList() {
        // "!!!" contains characters that cannot match any template position
        var results = suggestion.suggest("!!!");

        assertThat(results).isEmpty();
    }

    @Test
    void suggest_inputLongerThanLongestTemplate_returnsEmptyList() {
        // 11 characters — exceeds the longest template (10 chars)
        var results = suggestion.suggest("ABCDEFGHIJK");

        assertThat(results).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Interface default method: suggest(field, query)
    // -------------------------------------------------------------------------

    @Test
    void suggest_withFieldParam_delegatesToSuggestQuery() {
        var resultsDirect = suggestion.suggest("Q9");
        var resultsViaField = suggestion.suggest("Accession", "Q9");

        // Both lists must be non-null and respect format invariants
        assertThat(resultsDirect).isNotNull();
        assertThat(resultsViaField).isNotNull();

        resultsViaField.forEach(acc ->
                assertThat(ACCESSION_PATTERN.matcher(acc).matches()).isTrue()
        );
    }
}


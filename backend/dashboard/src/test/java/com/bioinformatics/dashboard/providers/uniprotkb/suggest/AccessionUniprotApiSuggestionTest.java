package com.bioinformatics.dashboard.providers.uniprotkb.suggest;

import com.bioinformatics.common.providers.uniprotkb.dto.UniProtLightEntry;
import com.bioinformatics.common.providers.uniprotkb.service.UniprotKbRestService;
import com.bioinformatics.common.uniprot.dto.UniprotKbResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AccessionUniprotApiSuggestion}.
 *
 * <p>The tested regex is:
 * {@code (?i)([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?}
 *
 * <p>Because {@link AccessionUniprotApiSuggestion#suggest} is non-deterministic,
 * tests assert invariants (format, count, containment) rather than exact values.
 */
@ExtendWith(MockitoExtension.class)
class AccessionUniprotApiSuggestionTest {

    private static final List<UniProtLightEntry> LIGHT_ENTRIES = List.of(
            lightEntry("P0A1C1"),
            lightEntry("P0A1C2"),
            lightEntry("P55717"),
            lightEntry("P80153"),
            lightEntry("P40290"),
            lightEntry("Q7ARI8"),
            lightEntry("E7CLN6"),
            lightEntry("P40291"),
            lightEntry("P47872"),
            lightEntry("Q5FWI2"),
            lightEntry("O46502"),
            lightEntry("P23811"),
            lightEntry("Q8GYW8"),
            lightEntry("E7CAU3"),
            lightEntry("Q8IWY4"),
            lightEntry("Q6NZL8"),
            lightEntry("Q5G872"),
            lightEntry("Q9NQ36"),
            lightEntry("Q1I172"),
            lightEntry("Q9JJS0")
    );
    /**
     * Same regex the SUT uses to validate generated accessions.
     */
    private static final Pattern ACCESSION_PATTERN = Pattern.compile(
            "^(?i)([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?$"
    );
    @Mock
    private UniprotKbRestService uniprotKbRestService;

    private static UniProtLightEntry lightEntry(String accession) {
        return new UniProtLightEntry(null, accession, null, null, null, null);
    }

    private static ResponseEntity<UniprotKbResponse<UniProtLightEntry>> okResponse(List<UniProtLightEntry> entries) {
        return ResponseEntity.ok(new UniprotKbResponse<>(entries));
    }

    private AccessionUniprotApiSuggestion suggestion;

    private static ResponseEntity<UniprotKbResponse<UniProtLightEntry>> getGeneratedSuggestion(InvocationOnMock invocation) {
        String capturedString = invocation.getArgument(0);
        List<String> originalAccessions = new ArrayList<>();
        Matcher matcher = Pattern.compile("accession:([^)]+)").matcher(capturedString);
        while (matcher.find()) {
            originalAccessions.add(matcher.group(1));
        }
        var entries = originalAccessions.stream()
                .map(AccessionUniprotApiSuggestionTest::lightEntry)
                .toList();
        return okResponse(entries);
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

    @BeforeEach
    void setUp() {
        suggestion = new AccessionUniprotApiSuggestion(uniprotKbRestService);
    }

    @Test
    void suggest_withValidPrefix_returnsUpToTenResults() {
        when(uniprotKbRestService.searchAll(anyString(), anyInt())).thenReturn(okResponse(LIGHT_ENTRIES));
        var results = suggestion.suggest("TP");

        assertThat(results)
                .isNotNull()
                .hasSizeLessThanOrEqualTo(10);
    }

    @RepeatedTest(5)
    void suggest_allResultsMatchAccessionPattern() {
        when(uniprotKbRestService.searchAll(anyString(), anyInt())).thenReturn(okResponse(LIGHT_ENTRIES));
        var results = suggestion.suggest("O1");

        assertThat(results).allSatisfy(accession ->
                assertThat(ACCESSION_PATTERN.matcher(accession).matches())
                        .as("Expected '%s' to match the UniProt accession pattern", accession)
                        .isTrue()
        );
    }

    @RepeatedTest(5)
    void suggest_resultsAreUnique() {
        when(uniprotKbRestService.searchAll(anyString(), anyInt()))
                .thenAnswer(AccessionUniprotApiSuggestionTest::getGeneratedSuggestion);
        var results = suggestion.suggest("A0");

        assertThat(results).doesNotHaveDuplicates();
    }

    @ParameterizedTest
    @ValueSource(strings = {"P2", "O1", "Q9", "A0"})
    void suggest_eachResultContainsInputSubstring(String query) {
        when(uniprotKbRestService.searchAll(anyString(), anyInt()))
                .thenAnswer(AccessionUniprotApiSuggestionTest::getGeneratedSuggestion);
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
        when(uniprotKbRestService.searchAll(anyString(), anyInt()))
                .thenAnswer(AccessionUniprotApiSuggestionTest::getGeneratedSuggestion);
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
        when(uniprotKbRestService.searchAll(anyString(), anyInt()))
                .thenAnswer(AccessionUniprotApiSuggestionTest::getGeneratedSuggestion);
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
        when(uniprotKbRestService.searchAll(anyString(), anyInt()))
                .thenAnswer(AccessionUniprotApiSuggestionTest::getGeneratedSuggestion);
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


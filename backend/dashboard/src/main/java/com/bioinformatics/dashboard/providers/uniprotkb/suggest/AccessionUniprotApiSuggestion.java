package com.bioinformatics.dashboard.providers.uniprotkb.suggest;

import com.bioinformatics.common.providers.uniprotkb.dto.UniProtLightEntry;
import com.bioinformatics.common.providers.uniprotkb.service.UniprotKbRestService;
import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import com.bioinformatics.dashboard.providers.uniprotkb.AbstractUniprotKbProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.bioinformatics.common.uniprot.UniprotMapperUtils.INACTIVE_ENTRY_TYPE;

/**
 * uniprot API suggestion provider for protein accessions.
 */
@Component
@RequiredArgsConstructor
public class AccessionUniprotApiSuggestion extends AbstractUniprotKbProvider implements SuggestionService {

    private static final int COUNT = 100;

    private static final String REGEX = "(?i)([OPQ][0-9][A-Z0-9]{3}[0-9]|[A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){1,2}[0-9])(-[0-9]+)?";
    private static final Pattern PATTERN = Pattern.compile("^" + REGEX + "$");

    private static final String OPQ = "OPQ";
    private static final String ANRZ = "ABCDEFGHIJKLMNRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final String[][] TEMPLATES = {
            // Template 0: [OPQ][0-9][A-Z0-9]{3}[0-9] (6 chars)
            {OPQ, DIGITS, ALPHANUM, ALPHANUM, ALPHANUM, DIGITS},

            // Template 1: [A-NR-Z][0-9][A-Z][A-Z0-9]{2}[0-9] (6 chars)
            {ANRZ, DIGITS, ALPHA, ALPHANUM, ALPHANUM, DIGITS},

            // Template 2: [A-NR-Z]([0-9][A-Z][A-Z0-9]{2}){2}[0-9] (10 chars)
            {ANRZ, DIGITS, ALPHA, ALPHANUM, ALPHANUM, DIGITS, ALPHA, ALPHANUM, ALPHANUM, DIGITS}
    };
    private final UniprotKbRestService uniprotKbRestService;

    private static List<Placement> findValidPlacements(String input) {
        var placements = new ArrayList<Placement>();

        for (var t = 0; t < TEMPLATES.length; t++) {
            var template = TEMPLATES[t];
            var maxOffset = template.length - input.length();

            for (var offset = 0; offset <= maxOffset; offset++) {
                if (matchesTemplateAt(input, template, offset)) {
                    placements.add(new Placement(t, offset));
                }
            }
        }
        return placements;
    }

    private static boolean matchesTemplateAt(String input, String[] template, int offset) {
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            var allowed = template[offset + i];
            if (allowed.indexOf(c) == -1) {
                return false;
            }
        }
        return true;
    }

    private static char getRandomChar(String pool, Random random) {
        return pool.charAt(random.nextInt(pool.length()));
    }

    @Override
    public String field() {
        return "Accession";
    }

    @Override
    public List<String> suggest(String query) {
        try {
            var suggestions = generateMatchingStrings(query)
                    .stream()
                    .map(e -> "(accession:" + e + ")")
                    .collect(Collectors.joining(" OR "));
            var result = uniprotKbRestService.searchAll(suggestions, 100);
            if (result.hasBody() && result.getBody() != null) {
                return result.getBody()
                        .results()
                        .stream()
                        .filter(e -> !INACTIVE_ENTRY_TYPE.equalsIgnoreCase(e.entryType()))
                        .map(UniProtLightEntry::primaryAccession)
                        .distinct()
                        .limit(10)
                        .toList();
            }
            return new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Generates {@value COUNT} unique strings matching the regex that contain/start with `input`.
     */
    private List<String> generateMatchingStrings(String input) {
        var results = new LinkedHashSet<String>();
        String normalizedInput = input.toUpperCase();
        var random = new Random();

        var validPlacements = findValidPlacements(normalizedInput);

        if (validPlacements.isEmpty()) {
            throw new IllegalArgumentException("Input '" + input + "' cannot fit into any valid pattern slot.");
        }

        int maxAttempts = COUNT * 1000;
        int attempts = 0;

        while (results.size() < COUNT && attempts < maxAttempts) {
            attempts++;
            var p = validPlacements.get(random.nextInt(validPlacements.size()));
            var template = TEMPLATES[p.templateIndex];

            var sb = new StringBuilder();
            for (var i = 0; i < p.offset; i++) {
                sb.append(getRandomChar(template[i], random));
            }

            sb.append(normalizedInput);

            var filledLength = p.offset + normalizedInput.length();
            for (var i = filledLength; i < template.length; i++) {
                sb.append(getRandomChar(template[i], random));
            }

            if (random.nextInt(4) == 0) {
                sb.append("-").append(random.nextInt(99) + 1);
            }

            var candidate = sb.toString();
            if (PATTERN.matcher(candidate).matches()) {
                results.add(candidate);
            }
        }

        return new ArrayList<>(results);
    }

    private record Placement(int templateIndex, int offset) {
    }

}

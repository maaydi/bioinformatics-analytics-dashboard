package com.bioinformatics.common.uniprot;


import com.bioinformatics.common.uniprot.dto.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UniprotMapperUtils {
    public static final String SWISSPROT_ENTRY_TYPE = "UniProtKB reviewed (Swiss-Prot)";
    public static final String INACTIVE_ENTRY_TYPE = "Inactive";
    public static final String GO_DATABASE = "GO";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Treats gene names from genes[1..n] as synonyms.
     * This is a best-effort approximation — synonym/ORF fields are not yet present in the DTO.
     */
    public static String[] extractGeneNameSynonyms(List<Gene> genes) {
        if (genes == null || genes.size() <= 1) return null;
        var synonyms = genes.stream()
                .skip(1)
                .map(Gene::geneName)
                .filter(Objects::nonNull)
                .map(GeneName::value)
                .filter(Objects::nonNull)
                .toArray(String[]::new);
        return synonyms.length == 0 ? null : synonyms;
    }

    public static String[] extractGeneOrfNames(List<Gene> genes) {
        if (genes == null || genes.isEmpty()) return null;
        var orfNames = genes.stream()
                .map(Gene::orfNames)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(GeneName::value)
                .filter(Objects::nonNull)
                .toArray(String[]::new);
        return orfNames.length == 0 ? null : orfNames;
    }

    public static CrossReferenceData extractCrossReferenceData(UniProtKBCrossReference ref) {
        char aspect = '?';
        var description = "";
        String secondaryId = null;
        String tertiaryInfo = null;
        if (ref.properties() != null) {
            if (!ref.properties().isEmpty()) secondaryId = ref.properties().get(0).value();
            if (ref.properties().size() > 1) tertiaryInfo = ref.properties().get(1).value();

            for (var prop : ref.properties()) {
                if ("GoTerm".equals(prop.key()) && prop.value() != null) {
                    int colon = prop.value().indexOf(':');
                    if (colon > 0) {
                        aspect = prop.value().charAt(0);
                        description = prop.value().substring(colon + 1).trim();
                    } else {
                        description = prop.value();
                    }
                    break;
                }
            }
        }
        return new CrossReferenceData(aspect, description, secondaryId, tertiaryInfo);
    }

    /**
     * Extracts a human-readable text representation from a polymorphic {@link Comment}.
     * UniProt comments carry their payload in different fields depending on the comment type.
     */
    public static String extractCommentText(Comment comment) {
        // Plain text comments (FUNCTION, CATALYTIC_ACTIVITY, SIMILARITY, …)
        if (comment.texts() != null && !comment.texts().isEmpty()) {
            var joined = comment.texts().stream()
                    .map(Text::value)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(" "));
            if (!joined.isBlank()) return joined;
        }
        // SUBCELLULAR_LOCATION
        if (comment.subcellularLocations() != null && !comment.subcellularLocations().isEmpty()) {
            var joined = comment.subcellularLocations().stream()
                    .filter(sl -> sl.location() != null)
                    .map(sl -> sl.location().value())
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("; "));
            if (!joined.isBlank()) return joined;
        }
        // DISEASE
        if (comment.disease() != null) {
            var desc = comment.disease().description();
            return desc != null ? desc : Objects.requireNonNullElse(comment.disease().diseaseId(), "");
        }
        // Note (fallback for structured notes)
        if (comment.note() != null && comment.note().texts() != null) {
            return comment.note().texts().stream()
                    .map(Text::value)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(" "));
        }
        return "";
    }

    public static String joinAuthors(Citation citation) {
        if (citation == null) return null;
        var authors = citation.authors();
        if (authors == null || authors.isEmpty()) {
            var groups = citation.authoringGroup();
            if (groups != null && !groups.isEmpty()) return String.join("; ", groups);
            return null;
        }
        return String.join(", ", authors);
    }

    public static String extractCitationXref(Citation citation, String database) {
        if (citation == null || citation.citationCrossReferences() == null) return null;
        return citation.citationCrossReferences().stream()
                .filter(x -> database.equals(x.database()))
                .map(CitationCrossReference::id)
                .findFirst()
                .orElse(null);
    }

    public static LocalDate auditDate(EntryAudit audit, AuditDateField field) {
        if (audit == null) return null;
        var raw = switch (field) {
            case FIRST_PUBLIC -> audit.firstPublicDate();
            case LAST_SEQUENCE_UPDATE -> audit.lastSequenceUpdateDate();
            case LAST_ANNOTATION_UPDATE -> audit.lastAnnotationUpdateDate();
        };
        return parseDate(raw);
    }


    // ── EntryAudit helpers ────────────────────────────────────────────────────

    public static Short auditShort(EntryAudit audit, AuditShortField field) {
        if (audit == null) return null;
        int value = switch (field) {
            case SEQUENCE_VERSION -> audit.sequenceVersion();
            case ENTRY_VERSION -> audit.entryVersion();
        };
        return (short) value;
    }

    public static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDate.parse(raw, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            // Tolerate partial dates such as "2023" or "2023-05"
            try {
                return LocalDate.parse(raw + "-01-01", DATE_FORMATTER);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    public static String joinEvidenceCodes(List<Evidence> evidences) {
        if (evidences == null || evidences.isEmpty()) return null;
        return evidences.stream()
                .map(Evidence::evidenceCode)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));
    }

    public enum AuditDateField {FIRST_PUBLIC, LAST_SEQUENCE_UPDATE, LAST_ANNOTATION_UPDATE}

    public enum AuditShortField {SEQUENCE_VERSION, ENTRY_VERSION}

    // ── Evidence codes ────────────────────────────────────────────────────────

    public record CrossReferenceData(char aspect, String description, String secondaryId, String tertiaryInfo) {
    }
}

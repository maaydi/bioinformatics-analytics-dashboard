package com.bioinformatics.dashboard.providers.uniprotkb.mapper;

import com.bioinformatics.dashboard.model.uniprot.dto.*;
import com.bioinformatics.dashboard.providers.postgres.gene.entity.*;
import com.bioinformatics.dashboard.providers.postgres.gene.entity.Keyword;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Maps a {@link UniProtEntry} (UniProtKB REST response DTO) to a {@link ProteinEntry} JPA aggregate.
 *
 * <p>Design notes:
 * <ul>
 *   <li>Child entities ({@link ProteinFeature}, {@link ProteinComment}, {@link ProteinPublication},
 *       {@link CrossReference}) have their {@code protein} back-reference set eagerly so callers
 *       can persist them directly via {@code ProteinAggregateItemWriter}.</li>
 *   <li>{@link GoTerm} and {@link com.bioinformatics.dashboard.providers.postgres.gene.entity.Keyword} objects are created without a DB id — the persistence
 *       layer is responsible for upsert logic (find-or-create).</li>
 *   <li>The {@link ProteinEntry#getHostOrganisms()} collection is intentionally left empty:
 *       host-organism data is not present in the current {@link UniProtEntry} DTO.</li>
 * </ul>
 */
@Component
public class UniProtEntryMapper {

    private static final String SWISSPROT_ENTRY_TYPE = "UniProtKB reviewed (Swiss-Prot)";
    private static final String GO_DATABASE = "GO";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Converts a {@link UniProtEntry} to a fully populated {@link ProteinEntry} aggregate.
     *
     * @param source non-null UniProt REST entry
     * @return a detached {@link ProteinEntry} ready for persistence
     */
    public ProteinEntry toProteinEntry(UniProtEntry source) {
        Objects.requireNonNull(source, "UniProtEntry must not be null");

        final ProteinEntry entry = buildCoreEntry(source);

        entry.setKeywords(mapKeywords(source.keywords()));
        entry.setFeatures(mapFeatures(source.features(), entry));
        entry.setGoTerms(mapGoTerms(source.uniProtKBCrossReferences()));
        entry.setCrossReferences(mapCrossReferences(source.uniProtKBCrossReferences(), entry));
        entry.setComments(mapComments(source.comments(), entry));
        entry.setPublications(mapPublications(source.references(), entry));

        return entry;
    }

    // ── Core scalar fields ────────────────────────────────────────────────────

    private ProteinEntry buildCoreEntry(UniProtEntry src) {
        return ProteinEntry.builder()
                .accession(src.primaryAccession())
                .entryName(src.uniProtkbId())
                .reviewed(SWISSPROT_ENTRY_TYPE.equals(src.entryType()))
                .integratedDate(auditDate(src.entryAudit(), AuditDateField.FIRST_PUBLIC))
                .sequenceDate(auditDate(src.entryAudit(), AuditDateField.LAST_SEQUENCE_UPDATE))
                .updatedDate(auditDate(src.entryAudit(), AuditDateField.LAST_ANNOTATION_UPDATE))
                .sequenceVersion(auditShort(src.entryAudit(), AuditShortField.SEQUENCE_VERSION))
                .entryVersion(auditShort(src.entryAudit(), AuditShortField.ENTRY_VERSION))
                .proteinFullName(extractProteinFullName(src.proteinDescription()))
                .geneNamePrimary(extractPrimaryGeneName(src.genes()))
                .geneNameSynonyms(extractGeneNameSynonyms(src.genes()))
                .geneOrfNames(extractGeneOrfNames(src.genes()))
                .geneOrderedLocus(null) // not present in current DTO
                .organismName(safeOrganism(src).scientificName())
                .organismCommonName(safeOrganism(src).commonName())
                .taxid(safeOrganism(src).taxonId())
                .lineage(lineageArray(src.organism()))
                .length(src.sequence() != null ? src.sequence().length() : 0)
                .molecularWeight(src.sequence() != null ? src.sequence().molWeight() : null)
                .sequenceChecksum(src.sequence() != null ? src.sequence().md5() : null)
                .sequence(src.sequence() != null ? src.sequence().value() : null)
                .evidenceLevel((short) src.annotationScore())
                .build();
    }

    // ── Protein name ──────────────────────────────────────────────────────────

    private String extractProteinFullName(ProteinDescription desc) {
        if (desc == null) return null;
        var rec = desc.recommendedName();
        if (rec == null) return null;
        var fn = rec.fullName();
        return fn != null ? fn.value() : null;
    }

    // ── Gene names ────────────────────────────────────────────────────────────

    /**
     * Returns the primary gene name from the first {@link Gene} in the list.
     * UniProt guarantees the first gene object carries the canonical name.
     */
    private String extractPrimaryGeneName(List<Gene> genes) {
        if (genes == null || genes.isEmpty()) return null;
        var primary = genes.getFirst();
        return primary.geneName() != null ? primary.geneName().value() : null;
    }

    /**
     * Treats gene names from genes[1..n] as synonyms.
     * This is a best-effort approximation — synonym/ORF fields are not yet present in the DTO.
     */
    private String[] extractGeneNameSynonyms(List<Gene> genes) {
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

    private String[] extractGeneOrfNames(List<Gene> genes) {
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

    // ── Organism helpers ──────────────────────────────────────────────────────

    /**
     * Returns a safe non-null placeholder organism when the source organism is absent.
     * Should not normally occur for well-formed UniProt entries.
     */
    private Organism safeOrganism(UniProtEntry src) {
        return src.organism() != null
                ? src.organism()
                : new Organism("Unknown", null, 0, List.of());
    }

    private String[] lineageArray(Organism organism) {
        if (organism == null || organism.lineage() == null) return new String[0];
        return organism.lineage().toArray(String[]::new);
    }

    // ── Keywords ──────────────────────────────────────────────────────────────

    /**
     * Creates transient {@link Keyword} entities. The persistence layer must resolve duplicates
     * via a find-or-create (upsert) strategy before flushing.
     */
    private List<Keyword> mapKeywords(
            List<com.bioinformatics.dashboard.model.uniprot.dto.Keyword> keywords) {
        if (keywords == null) return new ArrayList<>();
        return keywords.stream()
                .filter(k -> k.name() != null)
                .map(k -> Keyword.builder().name(k.name()).build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // ── Features ──────────────────────────────────────────────────────────────

    private Set<ProteinFeature> mapFeatures(List<Feature> features, ProteinEntry protein) {
        if (features == null) return new HashSet<>();
        return features.stream()
                .filter(f -> f.type() != null)
                .map(f -> toProteinFeature(f, protein))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private ProteinFeature toProteinFeature(Feature src, ProteinEntry protein) {
        Integer startPos = null;
        Integer endPos = null;
        if (src.location() != null) {
            if (src.location().start() != null) startPos = src.location().start().value();
            if (src.location().end() != null) endPos = src.location().end().value();
        }
        return ProteinFeature.builder()
                .protein(protein)
                .featureType(src.type())
                .startPos(startPos)
                .endPos(endPos)
                .note(src.description())
                .featureId(src.featureId())
                .evidence(joinEvidenceCodes(src.evidences()))
                .build();
    }

    // ── GO Terms (from cross-references where database = "GO") ────────────────

    private Set<GoTerm> mapGoTerms(List<UniProtKBCrossReference> crossRefs) {
        if (crossRefs == null) return new HashSet<>();
        return crossRefs.stream()
                .filter(r -> GO_DATABASE.equals(r.database()))
                .map(this::toGoTerm)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Extracts GO id, aspect, and description from a "GO" cross-reference.
     *
     * <p>Expected property format for key {@code "GoTerm"}: {@code "<aspect>:<description>"}
     * where aspect is {@code F} (Molecular Function), {@code P} (Biological Process),
     * or {@code C} (Cellular Component).
     */
    private GoTerm toGoTerm(UniProtKBCrossReference ref) {
        char aspect = '?';
        var description = "";
        if (ref.properties() != null) {
            for (CrossReferenceProperty prop : ref.properties()) {
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
        return GoTerm.builder()
                .goId(ref.id())
                .aspect(aspect)
                .description(description)
                .build();
    }

    // ── Cross-references (non-GO) ─────────────────────────────────────────────

    private Set<CrossReference> mapCrossReferences(
            List<UniProtKBCrossReference> crossRefs,
            ProteinEntry protein) {
        if (crossRefs == null) return new HashSet<>();
        return crossRefs.stream()
                .filter(r -> !GO_DATABASE.equals(r.database()))
                .map(r -> toCrossReference(r, protein))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private CrossReference toCrossReference(UniProtKBCrossReference src, ProteinEntry protein) {
        String secondaryId = null;
        String tertiaryInfo = null;
        if (src.properties() != null) {
            if (!src.properties().isEmpty()) secondaryId = src.properties().get(0).value();
            if (src.properties().size() > 1) tertiaryInfo = src.properties().get(1).value();
        }
        return CrossReference.builder()
                .protein(protein)
                .source(src.database())
                .identifier(src.id())
                .secondaryId(secondaryId)
                .tertiaryInfo(tertiaryInfo)
                .build();
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    private Set<ProteinComment> mapComments(List<Comment> comments, ProteinEntry protein) {
        if (comments == null) return new HashSet<>();
        return comments.stream()
                .filter(c -> c.commentType() != null)
                .map(c -> toProteinComment(c, protein))
                .filter(pc -> !pc.getText().isBlank())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private ProteinComment toProteinComment(Comment src, ProteinEntry protein) {
        return ProteinComment.builder()
                .protein(protein)
                .commentType(src.commentType())
                .text(extractCommentText(src))
                .build();
    }

    /**
     * Extracts a human-readable text representation from a polymorphic {@link Comment}.
     * UniProt comments carry their payload in different fields depending on the comment type.
     */
    private String extractCommentText(Comment comment) {
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

    // ── Publications ──────────────────────────────────────────────────────────

    private Set<ProteinPublication> mapPublications(List<Reference> references, ProteinEntry protein) {
        if (references == null) return new HashSet<>();
        return references.stream()
                .map(r -> toProteinPublication(r, protein))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private ProteinPublication toProteinPublication(Reference src, ProteinEntry protein) {
        var cit = src.citation();
        return ProteinPublication.builder()
                .protein(protein)
                .refNumber((short) src.referenceNumber())
                .pubmedId(extractCitationXref(cit, "PubMed"))
                .doi(extractCitationXref(cit, "DOI"))
                .authors(joinAuthors(cit))
                .title(cit != null ? cit.title() : null)
                .journal(cit != null ? cit.journal() : null)
                .build();
    }

    private String extractCitationXref(Citation citation, String database) {
        if (citation == null || citation.citationCrossReferences() == null) return null;
        return citation.citationCrossReferences().stream()
                .filter(x -> database.equals(x.database()))
                .map(CitationCrossReference::id)
                .findFirst()
                .orElse(null);
    }

    private String joinAuthors(Citation citation) {
        if (citation == null) return null;
        var authors = citation.authors();
        if (authors == null || authors.isEmpty()) {
            var groups = citation.authoringGroup();
            if (groups != null && !groups.isEmpty()) return String.join("; ", groups);
            return null;
        }
        return String.join(", ", authors);
    }

    // ── EntryAudit helpers ────────────────────────────────────────────────────

    private LocalDate auditDate(EntryAudit audit, AuditDateField field) {
        if (audit == null) return null;
        var raw = switch (field) {
            case FIRST_PUBLIC -> audit.firstPublicDate();
            case LAST_SEQUENCE_UPDATE -> audit.lastSequenceUpdateDate();
            case LAST_ANNOTATION_UPDATE -> audit.lastAnnotationUpdateDate();
        };
        return parseDate(raw);
    }

    private Short auditShort(EntryAudit audit, AuditShortField field) {
        if (audit == null) return null;
        int value = switch (field) {
            case SEQUENCE_VERSION -> audit.sequenceVersion();
            case ENTRY_VERSION -> audit.entryVersion();
        };
        return (short) value;
    }

    private LocalDate parseDate(String raw) {
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

    private String joinEvidenceCodes(List<Evidence> evidences) {
        if (evidences == null || evidences.isEmpty()) return null;
        return evidences.stream()
                .map(Evidence::evidenceCode)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));
    }

    private enum AuditDateField {FIRST_PUBLIC, LAST_SEQUENCE_UPDATE, LAST_ANNOTATION_UPDATE}

    // ── Evidence codes ────────────────────────────────────────────────────────

    private enum AuditShortField {SEQUENCE_VERSION, ENTRY_VERSION}
}


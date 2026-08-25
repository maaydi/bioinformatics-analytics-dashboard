package com.bioinformatics.dashboard.providers.uniprotkb.mapper;

import com.bioinformatics.common.uniprot.dto.*;
import com.bioinformatics.dashboard.model.gene.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bioinformatics.common.uniprot.UniprotMapperUtils.*;

/**
 * Maps a {@link UniProtEntry} (UniProtKB REST response DTO) to a {@link ProteinSummaryDto} and {@link ProteinDetailDto}.
 *
 * <p>Design notes:
 * <ul>
 *   <li>Child entities ({@link ProteinFeatureDto}, {@link ProteinCommentDto}, {@link ProteinPublicationDto},
 *       {@link CrossReferenceDto}) have their {@code protein} back-reference set eagerly so callers
 *       can persist them directly via {@code ProteinAggregateItemWriter}.</li>
 *   <li>{@link GoTermDto} and {@link KeywordDto} objects are created without a DB id — the persistence
 *       layer is responsible for upsert logic (find-or-create).</li>
 * </ul>
 */
@Component
public class UniProtProteinDtoMapper {

    // ── Public API ────────────────────────────────────────────────────────────

    public ProteinSummaryDto toSummary(UniProtEntry src) {
        return ProteinSummaryDto.builder()
                .accession(src.primaryAccession())
                .entryName(src.uniProtkbId())
                .proteinFullName(extractProteinFullName(src.proteinDescription()))
                .geneNamePrimary(extractPrimaryGeneName(src.genes()))
                .organismName(safeOrganism(src).scientificName())
                .taxid(safeOrganism(src).taxonId())
                .reviewed(SWISSPROT_ENTRY_TYPE.equals(src.entryType()))
                .length(src.sequence() != null ? src.sequence().length() : 0)
                .molecularWeight(src.sequence() != null ? src.sequence().molWeight() : null)
                .evidenceLevel((short) src.annotationScore())
                .keywords(keywordsToNames(mapKeywords(src.keywords())))
                .build();
    }

    public ProteinDetailDto toDetail(UniProtEntry src) {
        return ProteinDetailDto.builder()
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
                .keywords(keywordsToNames(mapKeywords(src.keywords())))
                .features(mapFeatures(src.features()))
                .goTerms(mapGoTerms(src.uniProtKBCrossReferences()))
                .crossReferences(mapCrossReferences(src.uniProtKBCrossReferences()))
                .comments(mapComments(src.comments()))
                .publications(mapPublications(src.references()))
                .hostOrganisms(new HashSet<>())
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
    private List<KeywordDto> mapKeywords(
            List<Keyword> keywords) {
        if (keywords == null) return new ArrayList<>();
        return keywords.stream()
                .filter(k -> k.name() != null)
                .map(k -> KeywordDto.builder().name(k.name()).build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // ── Features ──────────────────────────────────────────────────────────────

    private Set<ProteinFeatureDto> mapFeatures(List<Feature> features) {
        if (features == null) return new HashSet<>();
        return features.stream()
                .filter(f -> f.type() != null)
                .map(this::toProteinFeature)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private ProteinFeatureDto toProteinFeature(Feature src) {
        var builder = ProteinFeatureDto.builder()
                .featureType(src.type())
                .note(src.description())
                .featureId(src.featureId())
                .evidence(joinEvidenceCodes(src.evidences()));
        if (src.location() != null) {
            if (src.location().start() != null) builder.startPos(src.location().start().value());
            if (src.location().end() != null) builder.endPos(src.location().end().value());
        }
        return builder.build();
    }

    // ── GO Terms (from cross-references where database = "GO") ────────────────

    private Set<GoTermDto> mapGoTerms(List<UniProtKBCrossReference> crossRefs) {
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
    private GoTermDto toGoTerm(UniProtKBCrossReference ref) {
        var data = extractCrossReferenceData(ref);
        return GoTermDto.builder()
                .goId(ref.id())
                .aspect(data.aspect())
                .description(data.description())
                .build();
    }

    // ── Cross-references (non-GO) ─────────────────────────────────────────────

    private Set<CrossReferenceDto> mapCrossReferences(
            List<UniProtKBCrossReference> crossRefs) {
        if (crossRefs == null) return new HashSet<>();
        return crossRefs.stream()
                .filter(r -> !GO_DATABASE.equals(r.database()))
                .map(this::toCrossReference)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private CrossReferenceDto toCrossReference(UniProtKBCrossReference src) {
        var data = extractCrossReferenceData(src);
        return CrossReferenceDto.builder()
                .source(src.database())
                .identifier(src.id())
                .secondaryId(data.secondaryId())
                .tertiaryInfo(data.tertiaryInfo())
                .build();
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    private Set<ProteinCommentDto> mapComments(List<Comment> comments) {
        if (comments == null) return new HashSet<>();
        return comments.stream()
                .filter(c -> c.commentType() != null)
                .map(this::toProteinComment)
                .filter(pc -> !pc.text().isBlank())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private ProteinCommentDto toProteinComment(Comment src) {
        return ProteinCommentDto.builder()
                .commentType(src.commentType())
                .text(extractCommentText(src))
                .build();
    }

    // ── Publications ──────────────────────────────────────────────────────────

    private Set<ProteinPublicationDto> mapPublications(List<Reference> references) {
        if (references == null) return new HashSet<>();
        return references.stream()
                .map(this::toProteinPublication)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private ProteinPublicationDto toProteinPublication(Reference src) {
        var cit = src.citation();
        return ProteinPublicationDto.builder()
                .refNumber((short) src.referenceNumber())
                .pubmedId(extractCitationXref(cit, "PubMed"))
                .doi(extractCitationXref(cit, "DOI"))
                .authors(joinAuthors(cit))
                .title(cit != null ? cit.title() : null)
                .journal(cit != null ? cit.journal() : null)
                .build();
    }

    private List<String> keywordsToNames(List<KeywordDto> keywords) {
        if (keywords == null) return List.of();
        return keywords.stream().map(KeywordDto::name).toList();
    }
}


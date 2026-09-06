package com.bioinformatics.common.providers.uniprotkb.gene.specification;

import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.common.providers.uniprotkb.service.UniProtSearchFieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builds a UniProtKB REST API query string from a {@link GeneSearchRequest}.
 *
 * <p>Each  method returns an {@link Optional} query clause. The final query is
 * assembled by joining all non-empty clauses with {@code AND}.
 *
 * <p>Field term names follow the UniProtKB search field specification:
 * <a href="https://rest.uniprot.org/configure/uniprotkb/search-fields">search-fields</a>
 *
 * <p>Supported query syntax:
 * <ul>
 *   <li>Field query:   {@code field:value}</li>
 *   <li>Range query:   {@code field:[min TO max]}  — use {@code *} for open bounds</li>
 *   <li>Multi-value:   {@code (field:v1 OR field:v2)}</li>
 *   <li>Wildcard:      {@code field:prefix*}</li>
 *   <li>Global search: {@code (term*)}  — bare term with trailing wildcard</li>
 * </ul>
 *
 * <p><b>Limitation:</b> {@code goAspect} is intentionally not mapped. The UniProtKB
 * search API exposes no dedicated aspect filter; GO aspect (P/F/C) is encoded in the
 * GO term identifier itself and cannot be queried as a standalone field.
 */
@Component
@RequiredArgsConstructor
public class GeneSpecification {

    private final UniProtSearchFieldService uniProtSearchFieldService;


    /**
     * Assembles the complete UniProtKB query string from all active filters in
     * {@code req}. Returns {@code (*)} (match-all) when no filters are active or
     * when {@code req} is {@code null}.
     */
    public String fromRequest(GeneSearchRequest req) {
        if (req == null) {
            return "(*)";
        }

        var combined = Stream.of(
                        globalSearch(req.globalSearch()),
                        accession(req.accession()),
                        entryName(req.entryName()),
                        geneNamePrimary(req.geneNamePrimary()),
                        proteinFullName(req.proteinFullName()),
                        reviewed(req.reviewed()),
                        organism(req.organism()),
                        taxid(req.taxid()),
                        lineage(req.lineage()),
                        lengthBetween(req.lengthMin(), req.lengthMax()),
                        molecularWeightBetween(req.molecularWeightMin(), req.molecularWeightMax()),
                        evidenceLevels(req.evidenceLevels()),
                        keywords(req.keywords()),
                        goTermId(req.goTermId()),
                        featureType(req.featureType()),
                        crossRefSource(req.crossRefSource()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.joining(" AND "));

        return combined.isBlank() ? "(*)" : "(" + combined + ")";
    }

    /**
     * Free-text global search across all indexed fields.
     * Appends a wildcard so partial terms still match.
     * Example: {@code (kinase*)}
     */
    public Optional<String> globalSearch(String query) {
        if (!StringUtils.hasText(query)) return Optional.empty();
        var escaped = escapeSpecialChars(query.trim());
        return Optional.of("(" + escaped + "*)");
    }

    /**
     * Exact accession match.
     * Example: {@code accession:P12345}
     */
    public Optional<String> accession(String value) {
        if (!StringUtils.hasText(value)) return Optional.empty();
        return Optional.of("(accession:" + value.trim().toUpperCase() + ")");
    }

    /**
     * Entry name (UniProtKB {@code id} field) match with wildcard.
     * Example: {@code id:P53_HUMAN*}
     */
    public Optional<String> entryName(String value) {
        if (!StringUtils.hasText(value)) return Optional.empty();
        return Optional.of("(id:" + value.trim().toUpperCase() + "*)");
    }

    /**
     * Gene name match with wildcard.
     * Example: {@code gene:BRCA2*}
     */
    public Optional<String> geneNamePrimary(String value) {
        if (!StringUtils.hasText(value)) return Optional.empty();
        return Optional.of("(gene:" + value.trim() + "*)");
    }

    /**
     * Protein name match with wildcard.
     * Example: {@code protein_name:elastin*}
     */
    public Optional<String> proteinFullName(String value) {
        if (!StringUtils.hasText(value)) return Optional.empty();
        return Optional.of("(protein_name:" + value.trim() + "*)");
    }

    /**
     * Reviewed (Swiss-Prot curated) status filter.
     * Example: {@code reviewed:true}
     */
    public Optional<String> reviewed(Boolean value) {
        if (value == null) return Optional.empty();
        return Optional.of("(reviewed:" + value + ")");
    }

    /**
     * Organism name match with wildcard ({@code organism_name} field).
     * Example: {@code organism_name:human*}
     */
    public Optional<String> organism(String value) {
        if (!StringUtils.hasText(value)) return Optional.empty();
        return Optional.of("(organism_name:" + value.trim() + "*)");
    }

    /**
     * Organism taxonomy ID (NCBI taxon ID) exact match ({@code organism_id} field).
     * Example: {@code organism_id:9606}
     */
    public Optional<String> taxid(Integer value) {
        if (value == null) return Optional.empty();
        return Optional.of("(organism_id:" + value + ")");
    }

    /**
     * Taxonomic lineage match ({@code taxonomy_name} field, maps to the OC line in
     * UniProt flat file). Wildcard applied for partial matching.
     * Example: {@code taxonomy_name:mammalia*}
     */
    public Optional<String> lineage(String value) {
        if (!StringUtils.hasText(value)) return Optional.empty();
        return Optional.of("(taxonomy_name:" + value.trim() + "*)");
    }

    /**
     * Sequence length range filter ({@code length} field, type: range).
     * Open bounds use {@code *}.
     * Examples:
     * <ul>
     *   <li>Both bounds: {@code length:[100 TO 500]}</li>
     *   <li>Min only:    {@code length:[100 TO *]}</li>
     *   <li>Max only:    {@code length:[* TO 500]}</li>
     * </ul>
     */
    public Optional<String> lengthBetween(Integer min, Integer max) {
        if (min == null && max == null) return Optional.empty();
        var lo = min != null ? String.valueOf(min) : "*";
        var hi = max != null ? String.valueOf(max) : "*";
        return Optional.of("(length:[" + lo + " TO " + hi + "])");
    }

    /**
     * Molecular mass range filter in Daltons ({@code mass} field, type: range).
     * Open bounds use {@code *}.
     * Examples:
     * <ul>
     *   <li>Both bounds: {@code mass:[10000 TO 50000]}</li>
     *   <li>Min only:    {@code mass:[10000 TO *]}</li>
     * </ul>
     */
    public Optional<String> molecularWeightBetween(Integer min, Integer max) {
        if (min == null && max == null) return Optional.empty();
        var lo = min != null ? String.valueOf(min) : "*";
        var hi = max != null ? String.valueOf(max) : "*";
        return Optional.of("(mass:[" + lo + " TO " + hi + "])");
    }

    /**
     * Protein existence level filter (1–5, {@code existence} field).
     * Multiple levels are combined with {@code OR}.
     * Example: {@code (existence:1 OR existence:2)}
     *
     * <p>UniProt level semantics:
     * 1=Evidence at protein level, 2=Evidence at transcript level,
     * 3=Inferred from homology, 4=Predicted, 5=Uncertain.
     */
    public Optional<String> evidenceLevels(List<Integer> levels) {
        if (levels == null || levels.isEmpty()) return Optional.empty();
        var clause = levels.stream()
                .map(l -> "(existence:" + l + ")")
                .collect(Collectors.joining(" OR "));
        return Optional.of("(" + clause + ")");
    }

    /**
     * Keyword filter ({@code keyword} field). Multiple keywords are combined with
     * {@code OR}, each with a trailing wildcard.
     * Example: {@code (keyword:kinase* OR keyword:activator*)}
     */
    public Optional<String> keywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return Optional.empty();
        var clause = keywords.stream()
                .filter(StringUtils::hasText)
                .map(k -> "keyword:" + k.trim())
                .collect(Collectors.joining(" OR "));
        return clause.isBlank() ? Optional.empty() : Optional.of("(" + clause + ")");
    }

    /**
     * Gene Ontology term ID filter ({@code go} field).
     *
     * <p>The request uses the full {@code GO:NNNNNNN} format; the UniProtKB API
     * expects only the 7-digit numeric portion.
     * Example: request {@code GO:0009986} → query {@code go:0009986}
     */
    public Optional<String> goTermId(String goId) {
        if (!StringUtils.hasText(goId)) return Optional.empty();
        // Strip "GO:" prefix — UniProt expects "go:0009986", not "go:GO:0009986"
        var numericId = goId.startsWith("GO:") ? goId.substring(3) : goId;
        return Optional.of("(go:" + numericId + ")");
    }

    /**
     * Feature type filter. Maps the generic feature type name to the UniProtKB
     * {@code ft_<type>} field family with a wildcard to capture all descriptions.
     *
     * <p>Conversion: trim → lowercase → replace spaces with underscores → prefix {@code ft_}.
     * Examples: {@code CHAIN} → {@code ft_chain:*},
     * {@code ACT_SITE} → {@code ft_act_site:*},
     * {@code TRANSMEM} → {@code ft_transmem:*}
     */
    public Optional<String> featureType(String type) {
        if (!StringUtils.hasText(type)) return Optional.empty();
        var clause = uniProtSearchFieldService.getCachedFeatureTypes()
                .entrySet()
                .stream()
                .filter(e -> e.getKey().equalsIgnoreCase(type))
                .map(s -> s.getValue()
                        .stream().map(e -> e.concat(":*"))
                        .collect(Collectors.joining(" OR ")))
                .collect(Collectors.joining(" "));
        return clause.isBlank() ? Optional.empty() : Optional.of("(" + clause + ")");
    }

    /**
     * Cross-reference database filter ({@code database} field).
     * Example: {@code database:pdb}
     */
    public Optional<String> crossRefSource(String source) {
        if (!StringUtils.hasText(source)) return Optional.empty();
        return Optional.of("(database:" + source.trim().toLowerCase() + ")");
    }

    /**
     * Escapes Lucene/UniProt query special characters that would break query syntax.
     * Applied only to free-text global search values, not to field-prefixed terms.
     */
    private String escapeSpecialChars(String value) {
        return value
                .replaceAll("[+\\-!(){}\\[\\]^\"~?:\\\\/]", "\\\\$0")
                .replace("&&", "\\&&")
                .replace("||", "\\||");
    }
}

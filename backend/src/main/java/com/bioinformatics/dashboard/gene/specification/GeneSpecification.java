package com.bioinformatics.dashboard.gene.specification;

import com.bioinformatics.dashboard.gene.dto.GeneSearchRequest;
import com.bioinformatics.dashboard.gene.entity.ProteinEntry;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * JPA Specifications for dynamic multi-filter queries on {@link ProteinEntry}.
 *
 * <p>Each static method returns a single predicate.  Predicates are combined
 * by the service layer using {@code .and()} / {@code .or()}.
 *
 * <p>Filter fields and their semantics are defined in documentation/api-contract.md §1
 * (POST /api/genes/search) and documentation/overview.md §9.
 *
 * <p>Example usage:
 * <pre>{@code
 * Specification<ProteinEntry> spec = Specification.where(GeneSpecification.reviewed(true))
 *         .and(GeneSpecification.organism("Human"))
 *         .and(GeneSpecification.lengthBetween(100, 500));
 * }</pre>
 */
public final class GeneSpecification {

    private GeneSpecification() {
    }

    public static Specification<ProteinEntry> fromRequest(GeneSearchRequest req) {
        if (Objects.isNull(req)) {
            return (root, query, cb) -> cb.conjunction();
        }

        return Stream.of(globalSearch(req.globalSearch()),
                        accession(req.accession()),
                        entryName(req.entryName()),
                        geneNamePrimary(req.geneNamePrimary()),
                        proteinFullName(req.proteinFullName()),
                        reviewed(req.reviewed()),
                        organism(req.organism()),
                        taxid(req.taxid()),
                        lengthBetween(req.lengthMin(), req.lengthMax()),
                        molecularWeightBetween(req.molecularWeightMin(), req.molecularWeightMax()),
                        evidenceLevels(req.evidenceLevels()),
                        keywords(req.keywords()),
                        lineage(req.lineage()),
                        hasGoTermId(req.goTermId()),
                        goAspect(req.goAspect()),
                        featureType(req.featureType()),
                        crossRefSource(req.crossRefSource()))
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }

    public static Specification<ProteinEntry> globalSearch(String query) {
        if (!StringUtils.hasText(query)) return null;
        // Uses PostgreSQL ts-vector full-text search via native query fragment
        return (root, cq, cb) ->
                cb.isTrue(cb.function(
                        "fts_match", Boolean.class,
                        root.get("searchVector"),
                        cb.literal(query)));
    }

    public static Specification<ProteinEntry> accession(String value) {
        if (!StringUtils.hasText(value)) return null;
        return (root, cq, cb) ->
                cb.equal(root.get("accession"), value);
    }

    public static Specification<ProteinEntry> entryName(String value) {
        if (!StringUtils.hasText(value)) return null;
        return (root, cq, cb) ->
                cb.like(cb.lower(root.get("entryName")), "%" + value.toLowerCase() + "%");
    }

    public static Specification<ProteinEntry> geneNamePrimary(String value) {
        if (!StringUtils.hasText(value)) return null;
        return (root, cq, cb) ->
                cb.like(cb.lower(root.get("geneNamePrimary")), "%" + value.toLowerCase() + "%");
    }

    public static Specification<ProteinEntry> proteinFullName(String value) {
        if (!StringUtils.hasText(value)) return null;
        return (root, cq, cb) ->
                cb.like(cb.lower(root.get("proteinFullName")), "%" + value.toLowerCase() + "%");
    }

    public static Specification<ProteinEntry> reviewed(Boolean value) {
        if (value == null) return null;
        return (root, cq, cb) -> cb.equal(root.get("reviewed"), value);
    }

    public static Specification<ProteinEntry> organism(String value) {
        if (!StringUtils.hasText(value)) return null;
        return (root, cq, cb) ->
                cb.like(cb.lower(root.get("organismName")), "%" + value.toLowerCase() + "%");
    }

    public static Specification<ProteinEntry> taxid(Integer value) {
        if (value == null) return null;
        return (root, cq, cb) -> cb.equal(root.get("taxid"), value);
    }

    public static Specification<ProteinEntry> lengthBetween(Integer min, Integer max) {
        if (min == null && max == null) return null;
        return (root, cq, cb) -> {
            if (min != null && max != null) return cb.between(root.get("length"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("length"), min);
            return cb.lessThanOrEqualTo(root.get("length"), max);
        };
    }

    public static Specification<ProteinEntry> molecularWeightBetween(Integer min, Integer max) {
        if (min == null && max == null) return null;
        return (root, cq, cb) -> {
            if (min != null && max != null) return cb.between(root.get("molecularWeight"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("molecularWeight"), min);
            return cb.lessThanOrEqualTo(root.get("molecularWeight"), max);
        };
    }

    public static Specification<ProteinEntry> evidenceLevels(java.util.List<Integer> levels) {
        if (levels == null || levels.isEmpty()) return null;
        return (root, cq, cb) -> root.get("evidenceLevel").in(levels);
    }

    public static Specification<ProteinEntry> hasGoTermId(String goId) {
        if (!StringUtils.hasText(goId)) return null;
        return (root, cq, cb) -> {
            var join = root.join("goTerms", jakarta.persistence.criteria.JoinType.INNER);
            return cb.equal(join.get("goId"), goId);
        };
    }

    public static Specification<ProteinEntry> keywords(java.util.List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return null;
        return (root, query, cb) -> {
            query.distinct(true);
            var join = root.join("keywords", jakarta.persistence.criteria.JoinType.INNER);
            var preds = keywords.stream()
                    .filter(StringUtils::hasText)
                    .map(k -> cb.like(cb.lower(join.get("name")), "%" + k.toLowerCase() + "%"))
                    .toArray(jakarta.persistence.criteria.Predicate[]::new);
            if (preds.length == 0) return cb.conjunction();
            return cb.or(preds);
        };
    }

    public static Specification<ProteinEntry> lineage(String lineageValue) {
        if (!StringUtils.hasText(lineageValue)) return null;
        return (root, query, cb) -> {
            // Use PostgreSQL array_to_string to search within the lineage text[] column
            var arrayStr = cb.function("array_to_string", String.class, root.get("lineage"), cb.literal(","));
            return cb.like(cb.lower(arrayStr), "%" + lineageValue.toLowerCase() + "%");
        };
    }

    public static Specification<ProteinEntry> goAspect(String aspect) {
        if (!StringUtils.hasText(aspect)) return null;
        return (root, cq, cb) -> {
            var join = root.join("goTerms", jakarta.persistence.criteria.JoinType.INNER);
            return cb.equal(join.get("aspect"), aspect.charAt(0));
        };
    }

    public static Specification<ProteinEntry> featureType(String type) {
        if (!StringUtils.hasText(type)) return null;
        return (root, cq, cb) -> {
            var join = root.join("features", jakarta.persistence.criteria.JoinType.INNER);
            return cb.equal(join.get("featureType"), type);
        };
    }

    public static Specification<ProteinEntry> crossRefSource(String source) {
        if (!StringUtils.hasText(source)) return null;
        return (root, query, cb) ->
        {
            query.distinct(true);
            var crossRefJoin = root.join("crossReferences");
            return cb.equal(crossRefJoin.get("source"), source);
        };
    }
}

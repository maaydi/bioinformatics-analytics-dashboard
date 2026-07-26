package com.bioinformatics.dashboard.model.gene;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Request body for {@code POST /api/genes/search} and {@code POST /api/genes/export-csv}.
 *
 * <p>All field-level validation rules are defined in documentation/validation-rules.md §2.
 * Validation annotations here must match those rules exactly.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record GeneSearchRequest(

        @Size(max = 200, message = "Search query too long (max 200 characters)")
        String globalSearch,

        @Size(max = 20, message = "accession filter too long (max 20)")
        String accession,

        @Size(max = 50)
        String entryName,

        @Size(max = 100, message = "gene name filter too long (max 100)")
        String geneNamePrimary,

        String proteinFullName,

        Boolean reviewed,

        @Size(max = 300, message = "organism filter too long (max 300)")
        String organism,

        @Positive(message = "taxid must be a positive integer")
        Integer taxid,

        String lineage,

        @Min(value = 1, message = "lengthMin must be ≥ 1")
        Integer lengthMin,

        @Max(value = 100_000, message = "lengthMax must be ≤ 100000")
        Integer lengthMax,

        @Min(value = 1, message = "molecularWeightMin must be ≥ 1")
        Integer molecularWeightMin,

        Integer molecularWeightMax,

        List<@Min(1) @Max(5) Integer> evidenceLevels,

        @Size(max = 10, message = "Too many keyword filters (max 10)")
        List<@Size(max = 100) String> keywords,

        @Pattern(regexp = "GO:\\d{7}", message = "Invalid GO term ID format (expected GO:0000000)")
        String goTermId,

        @Pattern(regexp = "[PFC]", message = "goAspect must be P (Process), F (Function), or C (Component)")
        String goAspect,

        String featureType,

        String crossRefSource,

        @Min(value = 0, message = "page must be ≥ 0")
        Integer page,

        @Min(1) @Max(200)
        Integer size,

        String sort,

        @Pattern(regexp = "asc|desc")
        String direction
) {
    public GeneSearchRequest(Integer page, Integer size, String sort, String direction) {
        this(
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                page, size, sort, direction
        );
    }

    /**
     * Compare two lists ignore order, ignore duplicate elements, and ensure null safety
     *
     */
    private static <T> boolean listEquals(List<T> listA, List<T> listB) {
        if (listA == listB) return true;
        if (listA == null || listB == null) return false;

        var setA = new HashSet<T>(listA);
        var setB = new HashSet<T>(listB);

        return setA.equals(setB);
    }

    /**
     * Cross-field validation: if both lengthMin and lengthMax are provided, lengthMin must be ≤ lengthMax.
     */
    @AssertTrue(message = "lengthMin must be ≤ lengthMax")
    private boolean isLengthRangeValid() {
        if (lengthMin() == null || lengthMax() == null) return true;
        return lengthMin() <= lengthMax();
    }

    /**
     * Cross-field validation: if both molecularWeightMin and molecularWeightMax are provided,
     * molecularWeightMin must be ≤ molecularWeightMax.
     */
    @AssertTrue(message = "molecularWeightMin must be ≤ molecularWeightMax")
    private boolean isMolecularWeightRangeValid() {
        if (molecularWeightMin() == null || molecularWeightMax() == null) return true;
        return molecularWeightMin() <= molecularWeightMax();
    }


    public Pageable getRequestPage(Set<String> sortFields, String defaultSortField) {
        var dir = direction == null ? "asc" : direction;
        var direct = Sort.Direction.fromString(dir);

        var sortField = sort == null ? defaultSortField : sort;
        if (!sortFields.contains(sortField)) {
            throw new IllegalArgumentException("Invalid sort field: '" + sortField + "'. Allowed fields: " + sortFields);
        }

        return PageRequest.of(page, size, direct, sortField);
    }

    private static String fieldToString(String fieldName, Object value) {
        if (value == null) return "";
        if (value instanceof List<?> lst && lst.isEmpty()) return "";
        return String.format("%s=%s, ", fieldName, value);
    }

    @Override
    public @NonNull String toString() {
        return "GeneSearchRequest[" + fieldsToString() +
                ',' +
                PaginationToString() +
                "]";
    }

    private String fieldsToString() {
        String sb = "Fields=[" + fieldToString("globalSearch", globalSearch) +
                fieldToString("accession", accession) +
                fieldToString("entryName", entryName) +
                fieldToString("geneNamePrimary", geneNamePrimary) +
                fieldToString("proteinFullName", proteinFullName) +
                fieldToString("reviewed", reviewed) +
                fieldToString("organism", organism) +
                fieldToString("taxid", taxid) +
                fieldToString("lineage", lineage) +
                fieldToString("lengthMin", lengthMin) +
                fieldToString("lengthMax", lengthMax) +
                fieldToString("molecularWeightMin", molecularWeightMin) +
                fieldToString("molecularWeightMax", molecularWeightMax) +
                fieldToString("evidenceLevels", evidenceLevels) +
                fieldToString("keywords", keywords) +
                fieldToString("goTermId", goTermId) +
                fieldToString("goAspect", goAspect) +
                fieldToString("featureType", featureType) +
                fieldToString("crossRefSource", crossRefSource);
        var s = sb.trim();
        if (s.endsWith(",")) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "]";
    }

    private String PaginationToString() {
        String sb = "Pagination=[" + fieldToString("page", page) +
                fieldToString("size", size) +
                fieldToString("sort", sort) +
                fieldToString("direction", direction);
        var s = sb.trim();
        if (s.endsWith(",")) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "]";
    }

    public GeneSearchRequest withPage(Integer newPage) {
        return new GeneSearchRequest(
                globalSearch, accession, entryName, geneNamePrimary, proteinFullName, reviewed,
                organism, taxid, lineage, lengthMin, lengthMax, molecularWeightMin, molecularWeightMax,
                evidenceLevels, keywords, goTermId, goAspect, featureType, crossRefSource,
                newPage, size, sort, direction
        );
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GeneSearchRequest(
                String search, String accession1, String name, String namePrimary, String fullName, Boolean reviewed1,
                String organism1, Integer taxid1, String lineage1, Integer min, Integer max, Integer weightMin,
                Integer weightMax, List<Integer> levels, List<String> keywords1, String termId, String aspect,
                String type, String refSource, Integer page1, Integer size1, String sort1, String direction1
        ))) return false;
        return Objects.equals(sort(), sort1) && Objects.equals(page(), page1) && Objects.equals(size(), size1)
                && Objects.equals(taxid(), taxid1) && Objects.equals(lineage(), lineage1)
                && Objects.equals(organism(), organism1) && Objects.equals(goTermId(), termId)
                && Objects.equals(goAspect(), aspect) && Objects.equals(accession(), accession1)
                && Objects.equals(entryName(), name) && Objects.equals(reviewed(), reviewed1)
                && Objects.equals(direction(), direction1) && Objects.equals(lengthMin(), min)
                && Objects.equals(lengthMax(), max) && Objects.equals(featureType(), type)
                && Objects.equals(globalSearch(), search) && Objects.equals(crossRefSource(), refSource)
                && Objects.equals(geneNamePrimary(), namePrimary) && Objects.equals(proteinFullName(), fullName)
                && Objects.equals(molecularWeightMin(), weightMin) && Objects.equals(molecularWeightMax(), weightMax)
                && listEquals(keywords(), keywords1) && listEquals(evidenceLevels(), levels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(globalSearch(), accession(), entryName(), geneNamePrimary(), proteinFullName(), reviewed(),
                organism(), taxid(), lineage(), lengthMin(), lengthMax(), molecularWeightMin(), molecularWeightMax(),
                evidenceLevels(), keywords(), goTermId(), goAspect(), featureType(), crossRefSource(), page(), size(),
                sort(), direction());
    }
}

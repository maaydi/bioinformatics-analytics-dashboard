package com.bioinformatics.dashboard.gene.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
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
}

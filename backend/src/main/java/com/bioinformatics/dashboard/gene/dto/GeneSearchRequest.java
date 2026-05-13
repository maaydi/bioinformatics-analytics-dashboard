package com.bioinformatics.dashboard.gene.dto;

import jakarta.validation.constraints.*;

import java.util.List;

/**
 * Request body for {@code POST /api/genes/search} and {@code POST /api/genes/export-csv}.
 *
 * <p>All field-level validation rules are defined in documentation/validation-rules.md §2.
 * Validation annotations here must match those rules exactly.
 */
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

        @Min(1) @Max(500)
        Integer size,

        String sort,

        @Pattern(regexp = "asc|desc")
        String direction
) {
}

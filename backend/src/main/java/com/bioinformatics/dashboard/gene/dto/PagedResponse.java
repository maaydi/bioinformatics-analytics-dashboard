package com.bioinformatics.dashboard.gene.dto;

import java.util.List;

/**
 * Paginated list wrapper returned by all list endpoints.
 *
 * <p>Schema defined in documentation/api-contract.md — Shared Schemas — {@code PagedResponse<T>}.
 *
 * @param <T> element type (e.g. ProteinSummaryDto)
 */
public record PagedResponse<T>(
        List<T> content,
        int     page,
        int     size,
        long    totalElements,
        int     totalPages
) {

    /** Convenience factory from a Spring {@code Page}. */
    public static <T> PagedResponse<T> of(org.springframework.data.domain.Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}

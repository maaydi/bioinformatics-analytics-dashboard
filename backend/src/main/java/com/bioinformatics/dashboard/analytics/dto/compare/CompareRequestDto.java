package com.bioinformatics.dashboard.analytics.dto.compare;

import com.bioinformatics.dashboard.gene.dto.GeneSearchRequest;
import jakarta.validation.Valid;

/**
 * DTO for comparing two gene search filter results.
 *
 * @param setA first filter snapshot
 * @param setB second filter snapshot
 */
public record CompareRequestDto(@Valid GeneSearchRequest setA, @Valid GeneSearchRequest setB) {
}

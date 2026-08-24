package com.bioinformatics.dashboard.model.analytics.compare;

import com.bioinformatics.common.models.gene.GeneSearchRequest;
import jakarta.validation.Valid;

/**
 * DTO for comparing two gene search filter results.
 *
 * @param setA first filter snapshot
 * @param setB second filter snapshot
 */
public record CompareRequestDto(@Valid GeneSearchRequest setA, @Valid GeneSearchRequest setB) {
}

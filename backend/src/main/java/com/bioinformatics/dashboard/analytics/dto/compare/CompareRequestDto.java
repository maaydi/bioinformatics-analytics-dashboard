package com.bioinformatics.dashboard.analytics.dto.compare;

import com.bioinformatics.dashboard.gene.dto.GeneSearchRequest;
import jakarta.validation.Valid;

public record CompareRequestDto(@Valid GeneSearchRequest setA, @Valid GeneSearchRequest setB) {
}

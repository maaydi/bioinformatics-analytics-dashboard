package com.bioinformatics.dashboard.model.gene;

import lombok.Builder;

/**
 * Protein keyword/tag record used in protein detail and analytics aggregations.
 */
@Builder
public record KeywordDto(int id, String name) {
}

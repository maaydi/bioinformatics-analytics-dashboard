package com.bioinformatics.dashboard.model.analytics;

/**
 * Transfers occurrences of a specific domain keyword.
 */
public record KeywordFrequencyDto(String keyword, long count) {
}

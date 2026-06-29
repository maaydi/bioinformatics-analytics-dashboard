package com.bioinformatics.dashboard.analytics.dto;

/**
 * Transfers occurrences of a specific domain keyword.
 */
public record KeywordFrequencyDto(String keyword, long count) {
}

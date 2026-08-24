package com.bioinformatics.analyticsservice.models;

/**
 * Transfers occurrences of a specific domain keyword.
 */
public record KeywordFrequencyDto(String keyword, long count) {
}

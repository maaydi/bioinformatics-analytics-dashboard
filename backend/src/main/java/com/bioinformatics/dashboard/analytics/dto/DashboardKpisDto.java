package com.bioinformatics.dashboard.analytics.dto;

public record DashboardKpisDto(long totalProteins, long reviewedCount, long unreviewedCount, int organismCount,
                               int taxonCount, int avgLength, long avgMolecularWeight, int minLength,
                               int maxLength) {
}

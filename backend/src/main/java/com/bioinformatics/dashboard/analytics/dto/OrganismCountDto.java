package com.bioinformatics.dashboard.analytics.dto;

public record OrganismCountDto(String organismName,
                               int taxid,
                               int total,
                               int reviewedCount,
                               int unreviewedCount,
                               int avgLength
) {
}
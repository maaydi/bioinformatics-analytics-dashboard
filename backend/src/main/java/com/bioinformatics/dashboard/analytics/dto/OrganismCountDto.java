package com.bioinformatics.dashboard.analytics.dto;

public record OrganismCountDto(String organismName,
                               int taxid,
                               int total,
                               int reviewedCount,
                               int unreviewedCount,
                               int avgLength
) {

    /**
     * Constructor for Analytic Protein Repository implementation use
     *
     */
    public OrganismCountDto(
            String organismName,
            Integer taxid,
            Long total,
            Integer reviewedCount,
            Integer unreviewedCount,
            Integer avgLength
    ) {
        this(organismName, taxid, total.intValue(), reviewedCount, unreviewedCount, avgLength);
    }
}

package com.bioinformatics.dashboard.analytics.dto;


import com.bioinformatics.dashboard.analytics.model.EvidenceLevel;

public record EvidenceLevelDto(int evidenceLevel, String label, long count) {

    public EvidenceLevelDto(EvidenceLevel evidence, long count) {
        this(evidence.getId(), evidence.getLabel(), count);
    }
}

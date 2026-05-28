package com.bioinformatics.dashboard.analytics.model;

import lombok.Getter;

@Getter
public enum EvidenceLevel {
    PROTEIN_LEVEL(1, "Protein level"),
    TRANSCRIPT_LEVEL(2, "Transcript level"),
    HOMOLOGY(3, "Homology"),
    PREDICTED(4, "Predicted"),
    UNCERTAIN(5, "Uncertain");

    private final int id;
    private final String label;

    EvidenceLevel(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public static EvidenceLevel fromId(int id) {
        for (EvidenceLevel level : values()) {
            if (level.id == id) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown EvidenceLevel id: " + id);
    }
}

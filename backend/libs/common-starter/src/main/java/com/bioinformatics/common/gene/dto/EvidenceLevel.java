package com.bioinformatics.common.gene.dto;

import lombok.Getter;

import java.util.Arrays;

public enum EvidenceLevel {
    PROTEIN_LEVEL((short) 1, "Protein level"),
    TRANSCRIPT_LEVEL((short) 2, "Transcript level"),
    HOMOLOGY((short) 3, "Homology"),
    PREDICTED((short) 4, "Predicted"),
    UNCERTAIN((short) 5, "Uncertain"),
    UNKNOWN((short) 0, "Unknown");
    @Getter
    private final short level;
    @Getter
    private final String label;

    EvidenceLevel(short level, String label) {
        this.level = level;
        this.label = label;
    }

    public static EvidenceLevel valueOf(short level) {
        return Arrays.stream(values()).filter(e -> e.level == level).findFirst().orElse(UNKNOWN);
    }

}

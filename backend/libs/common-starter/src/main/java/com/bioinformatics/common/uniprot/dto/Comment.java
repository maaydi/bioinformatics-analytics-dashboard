package com.bioinformatics.common.uniprot.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.ArrayList;
import java.util.List;

public record Comment(
        String commentType,
        List<Text> texts,
        List<Interaction> interactions,
        List<SubcellularLocation> subcellularLocations,
        Disease disease,
        Note note
) {
    public record Note(List<Text> texts) {

        @JsonCreator
        public static Note fromString(String rawText) {
            return new Note(List.of(new Text(new ArrayList<>(), rawText)));
        }
    }
}

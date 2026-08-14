package com.bioinformatics.dashboard.model.uniprot.dto;

import java.util.List;

public record AlternativeSequence(String originalSequence, List<String> alternativeSequences) {
}

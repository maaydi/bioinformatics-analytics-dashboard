package com.bioinformatics.common.uniprot.dto;

import java.util.List;

public record AlternativeSequence(String originalSequence, List<String> alternativeSequences) {
}

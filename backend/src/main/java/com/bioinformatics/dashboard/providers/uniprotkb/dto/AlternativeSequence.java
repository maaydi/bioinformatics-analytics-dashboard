package com.bioinformatics.dashboard.providers.uniprotkb.dto;

import java.util.List;

public record AlternativeSequence(String originalSequence, List<String> alternativeSequences) {
}

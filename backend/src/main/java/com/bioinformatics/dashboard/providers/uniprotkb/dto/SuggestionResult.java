package com.bioinformatics.dashboard.providers.uniprotkb.dto;

import java.util.List;

public record SuggestionResult(String query, String dictionary, List<Suggestion> suggestions) {
}

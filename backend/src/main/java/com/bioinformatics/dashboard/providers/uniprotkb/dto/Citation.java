package com.bioinformatics.dashboard.providers.uniprotkb.dto;

import java.util.List;

public record Citation(
        String id,
        String citationType,
        List<String> authors,
        List<String> authoringGroup,
        List<CitationCrossReference> citationCrossReferences,
        String title,
        String publicationDate,
        String journal,
        String firstPage,
        String lastPage,
        String volume
) {
}

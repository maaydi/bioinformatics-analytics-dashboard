package com.bioinformatics.dashboard.model.uniprot.dto;

import java.util.List;

public record Facet(String label, String name, boolean allowMultipleSelection, List<FacetValue> values) {
}

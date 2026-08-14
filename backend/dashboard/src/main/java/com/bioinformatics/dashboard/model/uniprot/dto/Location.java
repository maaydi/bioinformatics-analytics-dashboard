package com.bioinformatics.dashboard.model.uniprot.dto;

import java.util.List;

public record Location(List<Evidence> evidences, String value, String id) {
}

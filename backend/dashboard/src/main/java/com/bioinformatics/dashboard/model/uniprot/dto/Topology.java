package com.bioinformatics.dashboard.model.uniprot.dto;

import java.util.List;

public record Topology(List<Evidence> evidences, String value, String id) {
}

package com.bioinformatics.dashboard.model.uniprot.dto;

import java.util.List;

public record Text(List<Evidence> evidences, String value) {
}
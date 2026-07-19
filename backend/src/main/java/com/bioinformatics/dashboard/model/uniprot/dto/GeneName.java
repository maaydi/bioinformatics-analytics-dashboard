package com.bioinformatics.dashboard.model.uniprot.dto;

import java.util.List;

public record GeneName(List<Evidence> evidences, String value) {
}

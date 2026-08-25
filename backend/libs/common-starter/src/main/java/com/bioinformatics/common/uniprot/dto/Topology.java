package com.bioinformatics.common.uniprot.dto;

import java.util.List;

public record Topology(List<Evidence> evidences, String value, String id) {
}

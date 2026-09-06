package com.bioinformatics.common.uniprot.dto;

import java.util.List;

public record Location(List<Evidence> evidences, String value, String id) {
}

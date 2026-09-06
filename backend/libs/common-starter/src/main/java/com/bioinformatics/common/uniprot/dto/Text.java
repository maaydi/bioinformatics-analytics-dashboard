package com.bioinformatics.common.uniprot.dto;

import java.util.List;

public record Text(List<Evidence> evidences, String value) {
}
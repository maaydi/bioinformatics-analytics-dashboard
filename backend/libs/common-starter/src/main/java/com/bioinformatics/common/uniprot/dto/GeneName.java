package com.bioinformatics.common.uniprot.dto;

import java.util.List;

public record GeneName(List<Evidence> evidences, String value) {
}

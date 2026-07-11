package com.bioinformatics.dashboard.providers.uniprotkb.dto;

import java.util.List;

public record GeneName(List<Evidence> evidences, String value) {
}

package com.bioinformatics.dashboard.providers.uniprotkb.dto;

import java.util.List;

public record Text(List<Evidence> evidences, String value) {
}
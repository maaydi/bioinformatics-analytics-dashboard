package com.bioinformatics.dashboard.providers.uniprotkb.dto;

import java.util.List;

public record Topology(List<Evidence> evidences, String value, String id) {
}

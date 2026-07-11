package com.bioinformatics.dashboard.providers.uniprotkb.dto;

import java.util.List;

public record Reference(
        int referenceNumber,
        Citation citation,
        List<String> referencePositions
) {
}

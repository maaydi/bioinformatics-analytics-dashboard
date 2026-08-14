package com.bioinformatics.dashboard.model.uniprot.dto;

import java.util.List;

public record Reference(
        int referenceNumber,
        Citation citation,
        List<String> referencePositions
) {
}

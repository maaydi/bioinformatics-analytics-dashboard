package com.bioinformatics.common.uniprot.dto;

import java.util.List;

public record Reference(
        int referenceNumber,
        Citation citation,
        List<String> referencePositions
) {
}

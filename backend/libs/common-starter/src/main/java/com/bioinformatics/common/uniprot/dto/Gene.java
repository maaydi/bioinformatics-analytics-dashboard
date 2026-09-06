package com.bioinformatics.common.uniprot.dto;

import java.util.List;

public record Gene(GeneName geneName, List<GeneName> orfNames) {
}
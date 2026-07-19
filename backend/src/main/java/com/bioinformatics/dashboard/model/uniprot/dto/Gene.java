package com.bioinformatics.dashboard.model.uniprot.dto;

import java.util.List;

public record Gene(GeneName geneName, List<GeneName> orfNames) {
}
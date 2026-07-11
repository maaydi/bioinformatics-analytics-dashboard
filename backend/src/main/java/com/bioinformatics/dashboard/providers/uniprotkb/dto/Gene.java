package com.bioinformatics.dashboard.providers.uniprotkb.dto;

import java.util.List;

public record Gene(GeneName geneName, List<GeneName> orfNames) {
}
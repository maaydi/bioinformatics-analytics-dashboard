package com.bioinformatics.dashboard.providers.uniprotkb.dto;

import java.util.List;

public record UniProtLightEntry(String uniProtkbId, List<FeatureLight> features, List<GeneLight> genes) {
}

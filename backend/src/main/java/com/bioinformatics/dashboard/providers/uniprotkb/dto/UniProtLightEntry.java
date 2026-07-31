package com.bioinformatics.dashboard.providers.uniprotkb.dto;

import com.bioinformatics.dashboard.model.uniprot.dto.ProteinDescription;

import java.util.List;

public record UniProtLightEntry(String uniProtkbId, List<FeatureLight> features, List<GeneLight> genes,
                                ProteinDescription proteinDescription) {
}

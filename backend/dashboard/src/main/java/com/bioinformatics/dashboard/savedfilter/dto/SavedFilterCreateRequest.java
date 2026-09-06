package com.bioinformatics.dashboard.savedfilter.dto;

import com.bioinformatics.common.models.gene.GeneSearchRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SavedFilterCreateRequest(@NotBlank @Size(max = 100) String name,
                                       @NotNull @Valid GeneSearchRequest filterJson) {
}

package com.bioinformatics.dashboard.providers.uniprotkb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CrossRefLightEntry(String abbrev) {
}

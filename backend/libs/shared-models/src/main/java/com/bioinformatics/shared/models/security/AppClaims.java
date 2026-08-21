package com.bioinformatics.shared.models.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum AppClaims {
    ROLE("role"),
    DATA_PROVIDER("dataProvider");
    private final String claim;
}

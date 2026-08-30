package com.bioinformatics.shared.models.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum AppClaims {
    USER_ID("userId"),
    ROLES("roles"),
    TYPE("type"),
    DATA_PROVIDER("dataProvider");
    private final String claim;
}

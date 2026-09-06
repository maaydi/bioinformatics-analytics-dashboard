package com.bioinformatics.shared.models.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TypeClaimValue {
    ACCESS_TOKEN("access"),
    REFRESH_TOKEN("refresh"),
    SERVICE_TOKEN("service");
    private final String value;
}

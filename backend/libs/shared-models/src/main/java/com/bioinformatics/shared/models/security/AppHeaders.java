package com.bioinformatics.shared.models.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.bioinformatics.shared.models.security.Constants.*;

@Getter
@RequiredArgsConstructor
public enum AppHeaders {

    USER_ID(USER_ID_HEADER, ""),
    USER_ROLE(USER_ROLE_HEADER, Constants.USER_ROLE),
    DATA_PROVIDER(DATA_PROVIDER_HEADER, POSTGRES_DATA_PROVIDER);

    private final String header;
    private final String defaultValue;
}

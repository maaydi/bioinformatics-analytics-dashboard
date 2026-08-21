package com.bioinformatics.authservice.dto;

import java.io.Serializable;

/**
 * Response body for login and refresh endpoints.
 *
 * <p>{@code expiresIn} is the access-token lifetime in seconds (not an epoch timestamp).
 * Clients must add it to the current time to determine the absolute expiry.
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType
) implements Serializable {

    /**
     * Factory for the standard {@code Bearer} scheme.
     */
    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, expiresIn, "Bearer");
    }

    /**
     * Factory for service-to-service tokens that do not include a refresh token.
     */
    public static TokenResponse serviceBearer(String accessToken, long expiresIn) {
        return new TokenResponse(accessToken, null, expiresIn, "Bearer");
    }
}


package com.bioinformatics.dashboard.auth.dto;

import java.io.Serializable;

/**
 * Response body for login and refresh endpoints.
 *
 * @see <a href="{@docRoot}/documentation/api-contract.md">API Contract §5</a>
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType
) implements Serializable {
    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, expiresIn, "Bearer");
    }
}

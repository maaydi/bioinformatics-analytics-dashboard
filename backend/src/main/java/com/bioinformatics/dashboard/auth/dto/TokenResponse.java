package com.bioinformatics.dashboard.auth.dto;

/**
 * Response body for login and refresh endpoints.
 *
 * @see documentation/api-contract.md §5
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType
) {
    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, expiresIn, "Bearer");
    }
}

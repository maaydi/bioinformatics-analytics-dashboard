package com.bioinformatics.dashboard.auth.dto;

/**
 * Response body for {@code POST /api/auth/password}.
 *
 * @see <a href="{@docRoot}/documentation/validation-rules.md">Validation Rules §4</a>
 */
public record ChangePasswordResponse(boolean success, String message
) {
    public static ChangePasswordResponse succeed() {
        return new ChangePasswordResponse(true, "");
    }
}

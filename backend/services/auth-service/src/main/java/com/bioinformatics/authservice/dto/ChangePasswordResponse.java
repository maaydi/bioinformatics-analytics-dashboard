package com.bioinformatics.authservice.dto;

/**
 * Response body for {@code PUT /api/v1/auth/password}.
 */
public record ChangePasswordResponse(boolean success, String message) {

    public static ChangePasswordResponse succeed() {
        return new ChangePasswordResponse(true, "Password changed successfully.");
    }
}


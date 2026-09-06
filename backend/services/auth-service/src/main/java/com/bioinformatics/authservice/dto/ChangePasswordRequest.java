package com.bioinformatics.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request body for {@code PUT /api/v1/auth/password}.
 *
 * <p>Password complexity is enforced here (registration / change flow).
 * Login deliberately skips complexity checks — only presence matters there.
 */
public record ChangePasswordRequest(

        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{12,}$",
                message = "Password must be at least 12 characters and contain at least one uppercase letter, one lowercase letter, and one digit"
        )
        String newPassword
) {
}


package com.bioinformatics.dashboard.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request body for {@code POST /api/auth/password}.
 *
 * <p>Only presence is validated here — credential correctness is verified by
 * bcrypt comparison in the service layer, which returns 401 on mismatch.
 * Password complexity constraints belong on registration, not update.
 *
 * @see <a href="{@docRoot}/documentation/validation-rules.md">Validation Rules §4</a>
 */
public record ChangePasswordRequest(
        @NotBlank(message = "New Password is required")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{12,}$",
                message = "Password must be at least 12 characters long and contain at least one uppercase letter, one lowercase letter, and one digit"
        )
        String newPassword,

        @NotBlank(message = "Old password is required")
        String currentPassword
) {
}

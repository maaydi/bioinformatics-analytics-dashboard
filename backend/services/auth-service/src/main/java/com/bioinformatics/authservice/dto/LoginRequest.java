package com.bioinformatics.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/auth/login}.
 *
 * <p>Only presence is validated here — credential correctness is verified by
 * bcrypt comparison in the service layer, which returns 401 on mismatch.
 * Password complexity constraints belong on registration, not login.
 */
public record LoginRequest(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {
}


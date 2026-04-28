package com.bioinformatics.dashboard.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints (public — no JWT required).
 *
 * <p>Contract: documentation/api-contract.md §5 — Authentication Endpoints.
 * <ul>
 *   <li>{@code POST /api/auth/login}   — credentials → JWT pair</li>
 *   <li>{@code POST /api/auth/refresh} — refresh token → new JWT pair</li>
 * </ul>
 *
 * <p>Validation rules: documentation/validation-rules.md §4.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    // TODO: inject AuthService

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Object request) {
        // TODO: implement — accepts LoginRequest, returns TokenResponse
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Object request) {
        // TODO: implement — accepts RefreshRequest, returns TokenResponse
        throw new UnsupportedOperationException("Not yet implemented");
    }
}

package com.bioinformatics.dashboard.auth.controller;

import com.bioinformatics.dashboard.audit.annotation.Auditable;
import com.bioinformatics.dashboard.audit.annotation.RateLimited;
import com.bioinformatics.dashboard.audit.dto.AuditAction;
import com.bioinformatics.dashboard.auth.dto.LoginRequest;
import com.bioinformatics.dashboard.auth.dto.RefreshRequest;
import com.bioinformatics.dashboard.auth.dto.TokenResponse;
import com.bioinformatics.dashboard.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    private final AuthService authService;

    @PostMapping("/login")
    @RateLimited(key = "login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @RateLimited(key = "login")
    @Auditable(action = AuditAction.TOKEN_REFRESH)
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }
}

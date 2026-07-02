package com.bioinformatics.dashboard.auth.controller;

import com.bioinformatics.dashboard.audit.annotation.Auditable;
import com.bioinformatics.dashboard.audit.annotation.RateLimited;
import com.bioinformatics.dashboard.auth.dto.*;
import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.auth.service.AuthService;
import com.bioinformatics.dashboard.model.audit.AuditAction;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller responsible for public authentication endpoints.
 *
 * <p>Handles standard user authentication flows, producing JWT pairs for authenticated sessions.
 * Important: These APIs are exposed globally and do not require existing authentication headers.</p>
 * <ul>
 *   <li>{@code POST /api/auth/login}   — Exchanges user credentials for an active token response</li>
 *   <li>{@code POST /api/auth/refresh} — Exchanges a valid refresh token for a new set of tokens</li>
 *   <li>{@code GET /api/auth/me}      — Fetch current authenticated user info</li>
 * </ul>
 *
 * <p>Consult detailed restrictions in {@code documentation/validation-rules.md} and payloads inside {@code documentation/api-contract.md}.
 * Delegates underlying user management and encryption logic to the {@link AuthService}.</p>
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
    @Auditable(action = AuditAction.TOKEN_REFRESH, targetId = "#currentUser.username")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request, @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(authService.refresh(request, currentUser));
    }

    @PostMapping("/logout")
    @RateLimited(key = "login")
    @Auditable(action = AuditAction.LOGOUT, targetId = "#currentUser.username")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AppUser currentUser) {
        authService.logout(currentUser);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/password")
    @RateLimited(key = "login")
    @Auditable(action = AuditAction.UPDATE_PASSWORD, targetId = "#currentUser.username")
    public ResponseEntity<ChangePasswordResponse> updatePassword(@Valid @RequestBody ChangePasswordRequest request, @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(authService.updatePassword(request, currentUser));
    }
}

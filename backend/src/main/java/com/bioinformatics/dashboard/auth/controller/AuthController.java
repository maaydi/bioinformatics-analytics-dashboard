package com.bioinformatics.dashboard.auth.controller;

import com.bioinformatics.dashboard.audit.annotation.Auditable;
import com.bioinformatics.dashboard.audit.annotation.RateLimited;
import com.bioinformatics.dashboard.audit.dto.AuditAction;
import com.bioinformatics.dashboard.auth.dto.*;
import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

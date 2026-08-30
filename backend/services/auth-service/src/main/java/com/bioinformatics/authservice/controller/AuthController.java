package com.bioinformatics.authservice.controller;

import com.bioinformatics.authservice.dto.*;
import com.bioinformatics.authservice.service.AuthService;
import com.bioinformatics.common.config.web.CurrentUser;
import com.bioinformatics.shared.models.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication API extracted from the monolith for ARCH-001 Phase 1.
 *
 * <p>Target endpoints are exposed under {@code /api/v1/auth/*}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CurrentUser UserPrincipal user) {
        authService.logout(user.id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/service-token")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TokenResponse> issueServiceToken(@CurrentUser UserPrincipal user) {
        return ResponseEntity.ok(authService.issueServiceToken(user.id()));
    }

    @PutMapping("/password")
    public ResponseEntity<ChangePasswordResponse> updatePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @CurrentUser UserPrincipal user
    ) {
        return ResponseEntity.ok(authService.updatePassword(request, user.id()));
    }
}


package com.bioinformatics.authservice.controller;

import com.bioinformatics.authservice.dto.*;
import com.bioinformatics.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.bioinformatics.shared.models.security.Constants.USER_ID_HEADER;

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
    public ResponseEntity<Void> logout(@RequestHeader(USER_ID_HEADER) String username) {
        authService.logout(username);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/service-token")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TokenResponse> issueServiceToken(@RequestHeader(USER_ID_HEADER) String username) {
        return ResponseEntity.ok(authService.issueServiceToken(username));
    }

    @PutMapping("/password")
    public ResponseEntity<ChangePasswordResponse> updatePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader(USER_ID_HEADER) String username
    ) {
        return ResponseEntity.ok(authService.updatePassword(request, username));
    }
}


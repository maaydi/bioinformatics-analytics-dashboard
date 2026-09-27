package com.bioinformatics.authservice.controller;

import com.bioinformatics.authservice.dto.*;
import com.bioinformatics.authservice.service.AuthService;
import com.bioinformatics.common.config.web.CurrentUser;
import com.bioinformatics.shared.models.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for authentication and authorization operations.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/v1/auth/login - Authenticate user credentials → access + refresh tokens
 *   <li>POST /api/v1/auth/refresh - Exchange refresh token → new access token
 *   <li>POST /api/v1/auth/logout - Revoke all tokens and clear session
 *   <li>POST /api/v1/auth/service-token - Issue long-lived service token (admin-only)
 *   <li>PUT /api/v1/auth/password - Change authenticated user's password
 * </ul>
 *
 * <p>Security:
 * <ul>
 *   <li>No sensitive data (passwords, tokens) logged or exposed
 *   <li>All endpoints use HTTPS in production
 *   <li>Rate limiting recommended on /login endpoint
 *   <li>CSRF protection via Spring Security
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates user with username and password.
     *
     * <p>On success, returns access token (short-lived) and refresh token (long-lived).
     * On failure, throws BadCredentialsException (HTTP 401).
     *
     * @param request username and password (not logged)
     * @return OK (200) with access and refresh tokens
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Refreshes access token using a valid refresh token.
     *
     * <p>Revokes old refresh token and issues new token pair.
     * On failure, throws BadCredentialsException (HTTP 401).
     *
     * @param request refresh token (not logged)
     * @return OK (200) with new access and refresh tokens
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /**
     * Revokes all tokens and clears session.
     *
     * <p>After logout, all existing tokens become invalid.
     * Client must authenticate again via /login to get new tokens.
     *
     * @param user authenticated user from security context
     * @return NO_CONTENT (204)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CurrentUser UserPrincipal user) {
        authService.logout(user.id());
        return ResponseEntity.noContent().build();
    }

    /**
     * Issues a long-lived service token for backend-to-backend authentication.
     *
     * <p>Service tokens have longer expiry and are useful for:
     * <ul>
     *   <li>Service-to-service API calls
     *   <li>Scheduled batch jobs
     *   <li>API integrations
     * </ul>
     *
     * <p>Only administrators can issue service tokens.
     *
     * @param user authenticated admin user
     * @return OK (200) with service token
     * @throws AccessDeniedException if user is not admin
     */
    @PostMapping("/service-token")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TokenResponse> issueServiceToken(@CurrentUser UserPrincipal user) {
        return ResponseEntity.ok(authService.issueServiceToken(user.id()));
    }

    /**
     * Updates authenticated user's password.
     *
     * <p>Requires current password for verification (defense against session hijacking).
     * On success, all existing tokens are revoked; user must re-authenticate.
     *
     * @param request current password + new password (not logged)
     * @param user    authenticated user
     * @return OK (200) with success message
     * @throws BadCredentialsException if current password is incorrect
     */
    @PutMapping("/password")
    public ResponseEntity<ChangePasswordResponse> updatePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @CurrentUser UserPrincipal user
    ) {
        return ResponseEntity.ok(authService.updatePassword(request, user.id()));
    }
}

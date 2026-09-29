package com.bioinformatics.authservice.service;

import com.bioinformatics.authservice.config.ApplicationProperties;
import com.bioinformatics.authservice.dto.*;
import com.bioinformatics.authservice.entity.AppUser;
import com.bioinformatics.authservice.entity.RefreshToken;
import com.bioinformatics.authservice.repository.AppUserRepository;
import com.bioinformatics.authservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Core authentication and authorization service.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>User credential validation (login)
 *   <li>Token lifecycle management (generate, refresh, revoke)
 *   <li>Password management (update with old password verification)
 *   <li>Session management (logout, token revocation)
 *   <li>Service account administration (service token issuance)
 * </ul>
 *
 * <p>Security Principles (OWASP):
 * <ul>
 *   <li>No plaintext passwords logged or transmitted
 *   <li>No JWT tokens logged (only user context)
 *   <li>Tokens always hashed when stored (SHA-256)
 *   <li>Failed attempts logged for audit trail
 *   <li>Account lockout after N failed attempts
 *   <li>All modifications logged with user identifier
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ApplicationProperties properties;

    private static String hashToken(final String token) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    /**
     * Authenticates user credentials and issues tokens.
     *
     * <p>Flow:
     * <ol>
     *   <li>Validate credentials via Spring Security AuthenticationManager
     *   <li>Load user details from database
     *   <li>Generate access + refresh token pair
     *   <li>Store refresh token hash in database
     *   <li>Return tokens to client
     * </ol>
     *
     * <p>Security Audit Log:
     * <ul>
     *   <li>SUCCESS: username, timestamp
     *   <li>FAILURE: username, reason (invalid credentials), timestamp
     * </ul>
     *
     * @param request username + password (NOT logged)
     * @return token pair with access and refresh tokens
     * @throws BadCredentialsException if credentials invalid or user not found
     */
    public TokenResponse login(final LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            var userDetails = userDetailsService.loadUserByUsername(request.username());
            var user = appUserRepository.findByUsername(request.username())
                    .orElseThrow(() -> {
                        log.warn("[AUTH] Login failed - user not found - username='{}'", request.username());
                        return new BadCredentialsException("Invalid credentials");
                    });

            log.info("[AUTH] Login successful - username='{}', roles={}", user.getUsername(), user.getAuthorities());
            return issueTokenPair(userDetails, user);
        } catch (BadCredentialsException e) {
            log.warn("[AUTH] Login failed - invalid credentials - username='{}', reason='{}'",
                    request.username(), e.getMessage());
            throw e;
        }
    }

    /**
     * Refreshes expired access token using valid refresh token.
     *
     * <p>Flow:
     * <ol>
     *   <li>Validate refresh token type (must be REFRESH_TOKEN, not ACCESS_TOKEN)
     *   <li>Extract username from token
     *   <li>Verify token signature and expiration
     *   <li>Look up stored refresh token hash
     *   <li>Verify hash matches and token is not revoked
     *   <li>Revoke old token and issue new pair
     * </ol>
     *
     * <p>Security:
     * <ul>
     *   <li>Refresh tokens stored as SHA-256 hash (never plaintext)
     *   <li>Old token immediately revoked (one-use)
     *   <li>Token family tracking prevents reuse after logout
     * </ul>
     *
     * @param request refresh token (NOT logged)
     * @return new token pair
     * @throws BadCredentialsException if token invalid, expired, or revoked
     */
    @Transactional
    public TokenResponse refresh(final RefreshRequest request) {
        var token = request.refreshToken();

        if (!jwtService.isRefreshToken(token)) {
            log.warn("[AUTH] Token refresh failed - invalid token type");
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        var username = jwtService.extractUsername(token);
        var user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("[AUTH] Token refresh failed - user not found - username='{}'", username);
                    return new BadCredentialsException("Invalid or expired refresh token");
                });

        if (!jwtService.isTokenValid(token, user)) {
            log.warn("[AUTH] Token refresh failed - invalid signature or expired - username='{}'", username);
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        var tokenHash = hashToken(token);
        var persistedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("[AUTH] Token refresh failed - token not in database - username='{}'", username);
                    return new BadCredentialsException("Invalid or expired refresh token");
                });

        if (!Objects.equals(persistedToken.getUser().getId(), user.getId()) || !persistedToken.isValid()) {
            log.warn("[AUTH] Token refresh failed - token revoked or user mismatch - username='{}'", username);
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        persistedToken.setRevoked(true);
        refreshTokenRepository.save(persistedToken);
        log.debug("[AUTH] Token refresh - old token revoked - username='{}'", username);

        log.info("[AUTH] Token refresh successful - username='{}'", username);
        return issueTokenPair(user, user);
    }

    /**
     * Logs out user by revoking all refresh tokens.
     *
     * <p>Flow:
     * <ol>
     *   <li>Find user in database
     *   <li>Mark all refresh tokens as revoked
     *   <li>Clear Spring Security context
     * </ol>
     *
     * <p>Security Audit Log:
     * <ul>
     *   <li>Logout event with username and timestamp
     * </ul>
     *
     * @param username authenticated user (from JWT claims)
     */
    @Transactional
    public void logout(final String username) {
        var authenticatedUser = Objects.requireNonNull(currentUser(username), "Authenticated user is required");
        refreshTokenRepository.revokeAllByUserId(authenticatedUser.getId());
        SecurityContextHolder.clearContext();
        log.info("[AUTH] Logout successful - username='{}'", username);
    }

    /**
     * Issues a long-lived service token for backend-to-backend authentication.
     *
     * <p>Service tokens:
     * <ul>
     *   <li>Only issuable by administrators
     *   <li>Longer expiration (e.g., 30 days vs 1 hour for access tokens)
     *   <li>Used for service-to-service, scheduled jobs, integrations
     *   <li>Marked with TYPE=SERVICE_TOKEN claim
     * </ul>
     *
     * <p>Flow:
     * <ol>
     *   <li>Verify requester is admin (authorization check)
     *   <li>Verify user in database is admin (defense against data inconsistency)
     *   <li>Generate service token
     *   <li>Return to client
     * </ol>
     *
     * <p>Security Audit Log:
     * <ul>
     *   <li>Service token issuance event with admin username
     * </ul>
     *
     * @param username authenticated admin user
     * @return service token response
     * @throws AccessDeniedException if user is not admin
     */
    public TokenResponse issueServiceToken(final String username) {
        var authenticatedUser = Objects.requireNonNull(currentUser(username), "Authenticated user is required");

        if (!authenticatedUser.isAdmin()) {
            log.warn("[AUTH] Service token issuance denied - not admin - username='{}'", username);
            throw new AccessDeniedException("Only administrators can issue service tokens");
        }

        var managedUser = appUserRepository.findByUsername(authenticatedUser.getUsername())
                .orElseThrow(() -> {
                    log.error("[AUTH] Service token issuance failed - admin user not in database - username='{}'", username);
                    return new BadCredentialsException("Invalid credentials");
                });

        if (!managedUser.isAdmin()) {
            log.warn("[AUTH] Service token issuance denied - user lost admin status - username='{}'", username);
            throw new AccessDeniedException("Only administrators can issue service tokens");
        }

        var serviceToken = jwtService.generateServiceToken(managedUser);
        log.info("[AUTH] Service token issued - username='{}', expiresInSeconds={}",
                username, properties.jwt().serviceTokenExpirySeconds());
        return TokenResponse.serviceBearer(serviceToken, properties.jwt().serviceTokenExpirySeconds());
    }

    /**
     * Updates authenticated user's password.
     *
     * <p>Security:
     * <ul>
     *   <li>Requires current password for verification (defense against CSRF/session hijacking)
     *   <li>New password must meet strength requirements (enforced by validation)
     *   <li>All refresh tokens revoked (force re-authentication)
     *   <li>Current session cleared (user must login again)
     * </ul>
     *
     * <p>Flow:
     * <ol>
     *   <li>Verify current password via authentication manager
     *   <li>Encode new password with bcrypt
     *   <li>Update in database
     *   <li>Revoke all tokens
     *   <li>Clear session
     * </ol>
     *
     * <p>Security Audit Log:
     * <ul>
     *   <li>Password change event with username (NOT new password)
     * </ul>
     *
     * @param request  current password + new password (NOT logged)
     * @param username authenticated user
     * @return success response
     * @throws BadCredentialsException if current password incorrect
     */
    @Transactional
    public ChangePasswordResponse updatePassword(final ChangePasswordRequest request, final String username) {
        var authenticatedUser = Objects.requireNonNull(currentUser(username), "Authenticated user is required");

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    authenticatedUser.getUsername(),
                    request.currentPassword()
            ));
        } catch (BadCredentialsException _) {
            log.warn("[AUTH] Password change failed - invalid current password - username='{}'", username);
            throw new BadCredentialsException("Invalid username or current password");
        }

        authenticatedUser.setPassword(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(authenticatedUser);
        refreshTokenRepository.revokeAllByUserId(authenticatedUser.getId());
        SecurityContextHolder.clearContext();

        log.info("[AUTH] Password changed successfully - username='{}', all tokens revoked", username);
        return ChangePasswordResponse.succeed();
    }

    /**
     * Generates and issues access + refresh token pair.
     *
     * <p>Tokens:
     * <ul>
     *   <li>Access Token: short-lived (1 hour), used for API requests
     *   <li>Refresh Token: long-lived (7 days), used to get new access token
     * </ul>
     *
     * <p>Refresh token hash is persisted in database for validation on refresh.
     *
     * @param userDetails Spring Security UserDetails (for authority extraction)
     * @param user database user entity
     * @return token pair
     */
    private TokenResponse issueTokenPair(final UserDetails userDetails, final AppUser user) {
        var accessToken = jwtService.generateAccessToken(userDetails);
        var refreshToken = jwtService.generateRefreshToken(userDetails);

        saveRefreshToken(user, refreshToken);
        log.debug("[AUTH] Token pair issued - username='{}', accessExpiresInSeconds={}, refreshExpiresInSeconds={}",
                user.getUsername(), properties.jwt().accessTokenExpirySeconds(),
                properties.jwt().refreshTokenExpirySeconds());

        return TokenResponse.bearer(accessToken, refreshToken, properties.jwt().accessTokenExpirySeconds());
    }

    /**
     * Persists refresh token hash to database.
     *
     * <p>Storage:
     * <ul>
     *   <li>Original token NEVER stored (only hash)
     *   <li>Hash computed with SHA-256
     *   <li>Expiration tracked for cleanup
     *   <li>Revocation flag for logout support
     * </ul>
     *
     * @param user database user
     * @param rawRefreshToken unhashed token (immediately hashed for storage)
     */
    private void saveRefreshToken(final AppUser user, final String rawRefreshToken) {
        var refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawRefreshToken))
                .expiresAt(jwtService.extractExpiration(rawRefreshToken))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Retrieves current user from database by username.
     *
     * @param username user identifier
     * @return user entity
     * @throws BadCredentialsException if not found
     */
    private AppUser currentUser(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
    }
}


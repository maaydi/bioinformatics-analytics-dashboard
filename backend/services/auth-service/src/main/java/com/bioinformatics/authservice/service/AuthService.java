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
 * Core authentication business service for login/refresh/logout/password update.
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

    public TokenResponse login(final LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        var userDetails = userDetailsService.loadUserByUsername(request.username());
        var user = appUserRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        return issueTokenPair(userDetails, user);
    }

    @Transactional
    public TokenResponse refresh(final RefreshRequest request) {
        var token = request.refreshToken();

        if (!jwtService.isRefreshToken(token)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        var username = jwtService.extractUsername(token);
        var user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));

        if (!jwtService.isTokenValid(token, user)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        var tokenHash = hashToken(token);
        var persistedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));

        if (!Objects.equals(persistedToken.getUser().getId(), user.getId()) || !persistedToken.isValid()) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        persistedToken.setRevoked(true);
        refreshTokenRepository.save(persistedToken);

        return issueTokenPair(user, user);
    }

    @Transactional
    public void logout(final String username) {
        var authenticatedUser = Objects.requireNonNull(currentUser(username), "Authenticated user is required");
        refreshTokenRepository.revokeAllByUserId(authenticatedUser.getId());
        SecurityContextHolder.clearContext();
    }

    public TokenResponse issueServiceToken(final String username) {
        var authenticatedUser = Objects.requireNonNull(currentUser(username), "Authenticated user is required");

        if (!authenticatedUser.isAdmin()) {
            throw new AccessDeniedException("Only administrators can issue service tokens");
        }

        var managedUser = appUserRepository.findByUsername(authenticatedUser.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!managedUser.isAdmin()) {
            throw new AccessDeniedException("Only administrators can issue service tokens");
        }

        var serviceToken = jwtService.generateServiceToken(managedUser);
        return TokenResponse.serviceBearer(serviceToken, properties.jwt().serviceTokenExpirySeconds());
    }

    @Transactional
    public ChangePasswordResponse updatePassword(final ChangePasswordRequest request, final String username) {
        var authenticatedUser = Objects.requireNonNull(currentUser(username), "Authenticated user is required");

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    authenticatedUser.getUsername(),
                    request.currentPassword()
            ));
        } catch (BadCredentialsException _) {
            throw new BadCredentialsException("Invalid username or current password");
        }
        authenticatedUser.setPassword(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(authenticatedUser);
        refreshTokenRepository.revokeAllByUserId(authenticatedUser.getId());

        log.info("Password updated for user {}", authenticatedUser.getUsername());
        SecurityContextHolder.clearContext();
        return ChangePasswordResponse.succeed();
    }

    private TokenResponse issueTokenPair(final UserDetails userDetails, final AppUser user) {
        var accessToken = jwtService.generateAccessToken(userDetails);
        var refreshToken = jwtService.generateRefreshToken(userDetails);

        saveRefreshToken(user, refreshToken);

        return TokenResponse.bearer(accessToken, refreshToken, properties.jwt().accessTokenExpirySeconds());
    }

    private void saveRefreshToken(final AppUser user, final String rawRefreshToken) {
        var refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawRefreshToken))
                .expiresAt(jwtService.extractExpiration(rawRefreshToken))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    private AppUser currentUser(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
    }
}


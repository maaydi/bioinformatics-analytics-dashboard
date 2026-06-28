package com.bioinformatics.dashboard.auth.service;

import com.bioinformatics.dashboard.auth.dto.*;
import com.bioinformatics.dashboard.auth.repository.AppUserRepository;
import com.bioinformatics.dashboard.exception.AccessDeniedException;
import com.bioinformatics.dashboard.exception.PasswordUpdateException;
import com.bioinformatics.dashboard.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication service.
 *
 * <ul>
 *   <li>Login: delegates to Spring Security {@link AuthenticationManager}, then issues JWT pair.</li>
 *   <li>Refresh: validates the refresh token type and expiry, then issues a new JWT pair.</li>
 * </ul>
 *
 * @see <a href="{@docRoot}/documentation/api-contract.md">API Contract §5</a>
 * @see <a href="{@docRoot}/documentation/validation-rules.md">Validation Rules §4</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AppUserRepository userRepository;

    @Value("${app.jwt.access-token-expiry-seconds:3600}")
    private long accessTokenExpirySeconds;

    /**
     * Authenticates the user and returns a JWT access/refresh token pair.
     *
     * @throws org.springframework.security.core.AuthenticationException on bad credentials
     */
    public TokenResponse login(LoginRequest request) {
        log.info("User login <{}>", request.username());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        var userDetails = userDetailsService.loadUserByUsername(request.username());
        return buildTokenResponse(userDetails);
    }

    /**
     * Validates the refresh token and issues a new JWT pair.
     *
     * @throws BadCredentialsException if token is
     *                                 invalid, expired, or not of type "refresh"
     */
    public TokenResponse refresh(RefreshRequest request) {
        var token = request.refreshToken();

        if (!jwtUtil.isRefreshToken(token)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        var username = jwtUtil.extractUsername(token);
        var userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtUtil.isTokenValid(token, userDetails)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        return buildTokenResponse(userDetails);
    }

    @Transactional
    public ChangePasswordResponse updatePassword(@Valid ChangePasswordRequest request) {

        var currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth == null) {
            log.warn("Security violation: Attempting changing password for unauthenticated user");
            throw new AccessDeniedException("Operation not allowed");
        }
        var username = currentAuth.getName();
        log.info("Attempting password update for user <{}>", username);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.currentPassword())
            );
            var user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

            user.setPassword(passwordEncoder.encode(request.newPassword()));
            userRepository.save(user);

            log.info("User <{}> password updated successfully", username);
            SecurityContextHolder.clearContext();
            return ChangePasswordResponse.succeed();

        } catch (BadCredentialsException e) {
            log.warn("Failed password update for <{}>: Bad credentials", username);
            throw new BadCredentialsException("Invalid username or current password");
        } catch (Exception e) {
            log.error("Unexpected error during password update for user <{}>", username, e);
            throw new PasswordUpdateException("An error occurred while updating the password. Please try again later.", e);
        }
    }

    private TokenResponse buildTokenResponse(UserDetails userDetails) {
        var accessToken = jwtUtil.generateAccessToken(userDetails);
        var refreshToken = jwtUtil.generateRefreshToken(userDetails);
        return TokenResponse.bearer(accessToken, refreshToken, accessTokenExpirySeconds);
    }
}

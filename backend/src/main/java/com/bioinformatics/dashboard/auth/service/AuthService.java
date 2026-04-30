package com.bioinformatics.dashboard.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.bioinformatics.dashboard.auth.dto.LoginRequest;
import com.bioinformatics.dashboard.auth.dto.RefreshRequest;
import com.bioinformatics.dashboard.auth.dto.TokenResponse;
import com.bioinformatics.dashboard.security.JwtUtil;

import lombok.RequiredArgsConstructor;

/**
 * Authentication service.
 *
 * <ul>
 *   <li>Login: delegates to Spring Security {@link AuthenticationManager}, then issues JWT pair.</li>
 *   <li>Refresh: validates the refresh token type and expiry, then issues a new JWT pair.</li>
 * </ul>
 *
 * @see documentation/api-contract.md §5
 * @see documentation/validation-rules.md §4
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    @Value("${app.jwt.access-token-expiry-seconds:3600}")
    private long accessTokenExpirySeconds;

    /**
     * Authenticates the user and returns a JWT access/refresh token pair.
     *
     * @throws org.springframework.security.core.AuthenticationException on bad credentials
     */
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        return buildTokenResponse(userDetails);
    }

    /**
     * Validates the refresh token and issues a new JWT pair.
     *
     * @throws org.springframework.security.authentication.BadCredentialsException if token is
     *         invalid, expired, or not of type "refresh"
     */
    public TokenResponse refresh(RefreshRequest request) {
        String token = request.refreshToken();

        if (!jwtUtil.isRefreshToken(token)) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Invalid or expired refresh token");
        }

        String username = jwtUtil.extractUsername(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtUtil.isTokenValid(token, userDetails)) {
            throw new org.springframework.security.authentication.BadCredentialsException(
                    "Invalid or expired refresh token");
        }

        return buildTokenResponse(userDetails);
    }

    private TokenResponse buildTokenResponse(UserDetails userDetails) {
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);
        return TokenResponse.bearer(accessToken, refreshToken, accessTokenExpirySeconds);
    }
}

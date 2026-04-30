package com.bioinformatics.dashboard.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * JWT utility — issues and validates HS256 tokens.
 *
 * <p>Token configuration (documentation/validation-rules.md §4):
 * <ul>
 *   <li>Algorithm: HS256</li>
 *   <li>Access token expiry: {@code app.jwt.access-token-expiry-seconds} (default 3600)</li>
 *   <li>Refresh token expiry: {@code app.jwt.refresh-token-expiry-seconds} (default 86400)</li>
 * </ul>
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long accessTokenExpirySeconds;
    private final long refreshTokenExpirySeconds;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiry-seconds:3600}") long accessTokenExpirySeconds,
            @Value("${app.jwt.refresh-token-expiry-seconds:86400}") long refreshTokenExpirySeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirySeconds = accessTokenExpirySeconds;
        this.refreshTokenExpirySeconds = refreshTokenExpirySeconds;
    }

    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(),
                Map.of("type", "access"),
                accessTokenExpirySeconds);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(),
                Map.of("type", "refresh"),
                refreshTokenExpirySeconds);
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token);
            return claims.getSubject().equals(userDetails.getUsername())
                    && !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Returns {@code true} if the token is a structurally valid, non-expired refresh token.
     * Checks the {@code type} claim equals {@code "refresh"}.
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return "refresh".equals(claims.get("type", String.class))
                    && !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String buildToken(String subject, Map<String, Object> extraClaims, long expirySeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claims(extraClaims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirySeconds)))
                .signWith(key)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

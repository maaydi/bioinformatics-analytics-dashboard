package com.bioinformatics.authservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Issues and validates HS256 access and refresh JWTs.
 */
@Service
public class JwtService {

    private static final int HS256_MIN_KEY_BYTES = 32;

    private final SecretKey signingKey;
    private final String issuer;
    @Getter
    private final long accessTokenExpirySeconds;
    private final long refreshTokenExpirySeconds;
    @Getter
    private final long serviceTokenExpirySeconds;

    public JwtService(
            @Value("${app.jwt.secret:change-this-secret-key-change-this-secret-key}") String secret,
            @Value("${app.jwt.issuer:bioinformatics-auth}") String issuer,
            @Value("${app.jwt.access-token-expiry-seconds:3600}") long accessTokenExpirySeconds,
            @Value("${app.jwt.refresh-token-expiry-seconds:86400}") long refreshTokenExpirySeconds,
            @Value("${app.jwt.service-token-expiry-seconds:300}") long serviceTokenExpirySeconds
    ) {
        this.signingKey = buildKey(secret);
        this.issuer = issuer;
        this.accessTokenExpirySeconds = accessTokenExpirySeconds;
        this.refreshTokenExpirySeconds = refreshTokenExpirySeconds;
        this.serviceTokenExpirySeconds = serviceTokenExpirySeconds;
    }

    private static SecretKey buildKey(final String secret) {
        var keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length >= HS256_MIN_KEY_BYTES) {
            return Keys.hmacShaKeyFor(keyBytes);
        }

        var padded = new byte[HS256_MIN_KEY_BYTES];
        System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
        return Keys.hmacShaKeyFor(padded);
    }

    public String generateAccessToken(final UserDetails userDetails) {
        var roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return buildToken(
                userDetails.getUsername(),
                Map.of("type", "access", "roles", roles),
                accessTokenExpirySeconds
        );
    }

    public String generateRefreshToken(final UserDetails userDetails) {
        return buildToken(
                userDetails.getUsername(),
                Map.of("type", "refresh"),
                refreshTokenExpirySeconds
        );
    }

    public String generateServiceToken(final UserDetails userDetails) {
        var roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return buildToken(
                userDetails.getUsername(),
                Map.of("type", "service", "roles", roles),
                serviceTokenExpirySeconds
        );
    }

    public String extractUsername(final String token) {
        return parseClaims(token).getSubject();
    }

    public Instant extractExpiration(final String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    public boolean isTokenValid(final String token, final UserDetails userDetails) {
        try {
            var claims = parseClaims(token);
            return claims.getSubject().equals(userDetails.getUsername())
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException _) {
            return false;
        }
    }

    public boolean isRefreshToken(final String token) {
        try {
            var claims = parseClaims(token);
            return "refresh".equals(claims.get("type", String.class))
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException _) {
            return false;
        }
    }

    private String buildToken(final String subject, final Map<String, Object> extraClaims, final long expirySeconds) {
        var now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(subject)
                .claims(extraClaims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirySeconds)))
                .signWith(signingKey)
                .compact();
    }

    private Claims parseClaims(final String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}


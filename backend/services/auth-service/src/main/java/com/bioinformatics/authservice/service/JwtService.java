package com.bioinformatics.authservice.service;

import com.bioinformatics.authservice.config.ApplicationProperties;
import com.bioinformatics.shared.models.security.AppClaims;
import com.bioinformatics.shared.models.security.TypeClaimValue;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class JwtService {
    private final ApplicationProperties properties;

    public String generateAccessToken(final UserDetails userDetails) {
        var roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return buildToken(
                userDetails.getUsername(),
                Map.of(AppClaims.TYPE.getClaim(), TypeClaimValue.ACCESS_TOKEN.getValue(), AppClaims.ROLES.getClaim(), roles),
                properties.jwt().accessTokenExpirySeconds()
        );
    }

    public String generateRefreshToken(final UserDetails userDetails) {
        return buildToken(
                userDetails.getUsername(),
                Map.of(AppClaims.TYPE.getClaim(), TypeClaimValue.REFRESH_TOKEN.getValue()),
                properties.jwt().refreshTokenExpirySeconds()
        );
    }

    public String generateServiceToken(final UserDetails userDetails) {
        var roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return buildToken(
                userDetails.getUsername(),
                Map.of(AppClaims.TYPE.getClaim(), TypeClaimValue.SERVICE_TOKEN.getValue(), AppClaims.ROLES.getClaim(), roles),
                properties.jwt().serviceTokenExpirySeconds()
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
            return TypeClaimValue.REFRESH_TOKEN.getValue().equals(claims.get(AppClaims.TYPE.getClaim(), String.class))
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException _) {
            return false;
        }
    }

    private String buildToken(final String subject, final Map<String, Object> extraClaims, final long expirySeconds) {
        var now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.jwt().issuer())
                .subject(subject)
                .audience().add(properties.jwt().audience())
                .and()
                .claims(extraClaims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirySeconds)))
                .signWith(buildSignKey())
                .compact();
    }

    private Claims parseClaims(final String token) {
        return Jwts.parser()
                .verifyWith(buildSignKey())
                .clockSkewSeconds(60)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey buildSignKey() {
        var hs256KeyBytes = properties.jwt().keyBytesLen();
        var keyBytes = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length >= hs256KeyBytes) {
            return Keys.hmacShaKeyFor(keyBytes);
        }

        var padded = new byte[hs256KeyBytes];
        System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
        return Keys.hmacShaKeyFor(padded);
    }

}


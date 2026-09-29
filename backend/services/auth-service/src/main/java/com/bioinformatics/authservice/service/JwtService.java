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
 * JWT token generation and validation service.
 *
 * <p>Handles lifecycle of access tokens, refresh tokens, and service tokens using HS256 algorithm.
 *
 * <p>Token Types:
 * <ul>
 *   <li><b>Access Token</b> - Short-lived (1 hour), used for API requests
 *   <li><b>Refresh Token</b> - Long-lived (7 days), used to obtain new access token
 *   <li><b>Service Token</b> - Long-lived (30 days), used for backend service authentication
 * </ul>
 *
 * <p>Security:
 * <ul>
 *   <li>HS256 (HMAC-SHA256) signature algorithm
 *   <li>Tokens never logged (even when DEBUG)
 *   <li>Key stored securely in configuration
 *   <li>Token validation includes expiration and signature checks
 *   <li>Clock skew tolerance: 60 seconds (for distributed system time drift)
 * </ul>
 *
 * <p>No logging of token content at any level (prevents accidental exposure in logs).
 */
@Service
@RequiredArgsConstructor
public class JwtService {
    private final ApplicationProperties properties;

    /**
     * Generates an access token for user authentication.
     *
     * <p>Claims:
     * <ul>
     *   <li>subject: username
     *   <li>roles: user's granted authorities
     *   <li>type: ACCESS_TOKEN
     *   <li>exp: current time + accessTokenExpirySeconds
     * </ul>
     *
     * @param userDetails Spring Security UserDetails (for authority extraction)
     * @return JWT access token (never logged)
     */
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

    /**
     * Generates a refresh token for obtaining new access tokens.
     *
     * <p>Claims:
     * <ul>
     *   <li>subject: username
     *   <li>type: REFRESH_TOKEN
     *   <li>exp: current time + refreshTokenExpirySeconds
     * </ul>
     *
     * <p>Refresh token is persisted as SHA-256 hash in database for validation.
     *
     * @param userDetails Spring Security UserDetails
     * @return JWT refresh token (never logged)
     */
    public String generateRefreshToken(final UserDetails userDetails) {
        return buildToken(
                userDetails.getUsername(),
                Map.of(AppClaims.TYPE.getClaim(), TypeClaimValue.REFRESH_TOKEN.getValue()),
                properties.jwt().refreshTokenExpirySeconds()
        );
    }

    /**
     * Generates a service token for backend-to-backend authentication.
     *
     * <p>Service tokens:
     * <ul>
     *   <li>Longer expiration than access tokens (for batch jobs, integrations)
     *   <li>Carry full user roles and permissions
     *   <li>Marked with TYPE=SERVICE_TOKEN for identification
     *   <li>Only issuable by administrators
     * </ul>
     *
     * <p>Claims:
     * <ul>
     *   <li>subject: username (service account)
     *   <li>roles: user's authorities
     *   <li>type: SERVICE_TOKEN
     *   <li>exp: current time + serviceTokenExpirySeconds
     * </ul>
     *
     * @param userDetails Spring Security UserDetails
     * @return JWT service token (never logged)
     */
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

    /**
     * Extracts username (subject) claim from token.
     *
     * @param token JWT token
     * @return username
     */
    public String extractUsername(final String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extracts expiration claim from token.
     *
     * @param token JWT token
     * @return expiration instant
     */
    public Instant extractExpiration(final String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    /**
     * Validates token signature, expiration, and username match.
     *
     * <p>Validation steps:
     * <ul>
     *   <li>Verify HS256 signature with configured secret
     *   <li>Check token not expired (with 60-second clock skew)
     *   <li>Verify username matches userDetails
     * </ul>
     *
     * <p>Returns false (not throws) to allow controllers to return proper HTTP 401.
     *
     * @param token       JWT token
     * @param userDetails Spring Security UserDetails to match
     * @return true if valid, false otherwise
     */
    public boolean isTokenValid(final String token, final UserDetails userDetails) {
        try {
            var claims = parseClaims(token);
            return claims.getSubject().equals(userDetails.getUsername())
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException _) {
            return false;
        }
    }

    /**
     * Validates token is a refresh token (not access/service token).
     *
     * <p>Checks:
     * <ul>
     *   <li>TYPE claim equals REFRESH_TOKEN
     *   <li>Token not expired
     * </ul>
     *
     * <p>Prevents using access tokens as refresh tokens (security check).
     *
     * @param token JWT token
     * @return true if valid refresh token, false otherwise
     */
    public boolean isRefreshToken(final String token) {
        try {
            var claims = parseClaims(token);
            return TypeClaimValue.REFRESH_TOKEN.getValue().equals(claims.get(AppClaims.TYPE.getClaim(), String.class))
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException _) {
            return false;
        }
    }

    /**
     * Builds a signed JWT token with specified claims and expiration.
     *
     * <p>Claims structure:
     * <ul>
     *   <li>jti: unique token ID (UUID)
     *   <li>iss: issuer (from configuration)
     *   <li>sub: subject (username)
     *   <li>aud: audience (from configuration)
     *   <li>iat: issued at (current time)
     *   <li>exp: expiration (iat + expirySeconds)
     *   <li>additional custom claims (roles, type, etc.)
     * </ul>
     *
     * <p>Signed with HS256 using configured secret key.
     *
     * @param subject username
     * @param extraClaims custom claims (roles, type, etc.)
     * @param expirySeconds token lifetime
     * @return signed JWT
     */
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

    /**
     * Parses and validates JWT signature.
     *
     * <p>Validation:
     * <ul>
     *   <li>Verifies HS256 signature with configured key
     *   <li>Allows 60-second clock skew (for distributed time drift)
     * </ul>
     *
     * <p>Throws JwtException on validation failure.
     *
     * @param token JWT token
     * @return parsed Claims
     * @throws JwtException if signature invalid or token malformed
     */
    private Claims parseClaims(final String token) {
        return Jwts.parser()
                .verifyWith(buildSignKey())
                .clockSkewSeconds(60)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Builds the HS256 signing key from configured secret.
     *
     * <p>Key handling:
     * <ul>
     *   <li>If secret >= keyBytesLen: use as-is
     *   <li>If secret < keyBytesLen: zero-pad to required length
     * </ul>
     *
     * @return HMAC-SHA256 SecretKey
     */
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


package com.bioinformatics.authservice.service;

import com.bioinformatics.authservice.config.ApplicationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String TEST_SECRET = "my-secret-key-that-is-long-enough-for-hs256-algorithm";
    private static final String TEST_ISSUER = "test-issuer";
    private static final String TEST_AUDIENCE = "test-audience";
    private static final int EXPIRY_SECONDS = 3600;
    private static final int KEY_BYTES_LEN = 32;

    private JwtService jwtService;
    private ApplicationProperties properties;
    private UserDetails testUser;

    @BeforeEach
    void setUp() {
        var jwt = new ApplicationProperties.Jwt(
                TEST_SECRET, TEST_ISSUER, TEST_AUDIENCE,
                3600, 86400, 2592000, KEY_BYTES_LEN
        );
        var token = new ApplicationProperties.Token(true, 5);
        var password = new ApplicationProperties.Password(12, true, true, true, 12);
        var rateLimiter = new ApplicationProperties.RateLimiter(false, null, List.of());
        var auditPool = new ApplicationProperties.AuditPoolSettings(2, 4, 100, "auth-");

        properties = new ApplicationProperties(jwt, token, password, rateLimiter, auditPool);
        jwtService = new JwtService(properties);

        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        testUser = new User("alice", "password", authorities);
    }

    @Test
    void generateAccessToken_createsValidToken() {
        var token = jwtService.generateAccessToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
    }

    @Test
    void generateAccessToken_includesAccessTokenType() {
        var token = jwtService.generateAccessToken(testUser);
        var username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("alice");
        assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
    }

    @Test
    void generateRefreshToken_createsValidToken() {
        var token = jwtService.generateRefreshToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtService.isRefreshToken(token)).isTrue();
    }

    @Test
    void generateServiceToken_createsValidToken() {
        var token = jwtService.generateServiceToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
    }

    @Test
    void extractUsername_returnsCorrectUsername() {
        var token = jwtService.generateAccessToken(testUser);
        var username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("alice");
    }

    @Test
    void extractExpiration_returnsValidInstant() {
        var token = jwtService.generateAccessToken(testUser);
        var expiration = jwtService.extractExpiration(token);

        assertThat(expiration).isAfter(Instant.now());
    }

    @Test
    void isTokenValid_returnsTrueForValidToken() {
        var token = jwtService.generateAccessToken(testUser);

        assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForExpiredToken() {
        var expiredJwt = new ApplicationProperties.Jwt(
                TEST_SECRET, TEST_ISSUER, TEST_AUDIENCE,
                0, 86400, 2592000, KEY_BYTES_LEN
        );
        var expiredProperties = new ApplicationProperties(
                expiredJwt,
                properties.token(),
                properties.password(),
                properties.rateLimiter(),
                properties.auditPool()
        );
        var expiredService = new JwtService(expiredProperties);
        var token = expiredService.generateAccessToken(testUser);

        assertThat(jwtService.isTokenValid(token, testUser)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForWrongUsername() {
        var token = jwtService.generateAccessToken(testUser);
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var otherUser = new User("bob", "password", authorities);

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForMalformedToken() {
        assertThat(jwtService.isTokenValid("invalid-token", testUser)).isFalse();
    }

    @Test
    void isRefreshToken_returnsTrueForValidRefreshToken() {
        var token = jwtService.generateRefreshToken(testUser);

        assertThat(jwtService.isRefreshToken(token)).isTrue();
    }

    @Test
    void isRefreshToken_returnsFalseForAccessToken() {
        var token = jwtService.generateAccessToken(testUser);

        assertThat(jwtService.isRefreshToken(token)).isFalse();
    }

    @Test
    void isRefreshToken_returnsFalseForMalformedToken() {
        assertThat(jwtService.isRefreshToken("invalid-token")).isFalse();
    }

    @Test
    void generatedTokensHaveDifferentExpirations() {
        var accessToken = jwtService.generateAccessToken(testUser);
        var refreshToken = jwtService.generateRefreshToken(testUser);

        var accessExpiry = jwtService.extractExpiration(accessToken);
        var refreshExpiry = jwtService.extractExpiration(refreshToken);

        assertThat(refreshExpiry).isAfter(accessExpiry);
    }

    @Test
    void multipleTokensForSameUserAreDifferent() {
        var token1 = jwtService.generateAccessToken(testUser);
        var token2 = jwtService.generateAccessToken(testUser);

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void serviceTokenHasLongerExpiration() {
        var accessToken = jwtService.generateAccessToken(testUser);
        var serviceToken = jwtService.generateServiceToken(testUser);

        var accessExpiry = jwtService.extractExpiration(accessToken);
        var serviceExpiry = jwtService.extractExpiration(serviceToken);

        assertThat(serviceExpiry).isAfter(accessExpiry);
    }
}


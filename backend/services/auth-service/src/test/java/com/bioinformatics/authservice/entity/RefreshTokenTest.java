package com.bioinformatics.authservice.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    void isExpired_futureExpiration_returnsFalse() {
        var token = RefreshToken.builder()
                .id(1L)
                .tokenHash("hash")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        assertThat(token.isExpired()).isFalse();
    }

    @Test
    void isExpired_pastExpiration_returnsTrue() {
        var token = RefreshToken.builder()
                .id(1L)
                .tokenHash("hash")
                .expiresAt(Instant.now().minusSeconds(3600))
                .revoked(false)
                .build();

        assertThat(token.isExpired()).isTrue();
    }

    @Test
    void isValid_notRevokedAndNotExpired_returnsTrue() {
        var token = RefreshToken.builder()
                .id(1L)
                .tokenHash("hash")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        assertThat(token.isValid()).isTrue();
    }

    @Test
    void isValid_revoked_returnsFalse() {
        var token = RefreshToken.builder()
                .id(1L)
                .tokenHash("hash")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build();

        assertThat(token.isValid()).isFalse();
    }

    @Test
    void isValid_expired_returnsFalse() {
        var token = RefreshToken.builder()
                .id(1L)
                .tokenHash("hash")
                .expiresAt(Instant.now().minusSeconds(3600))
                .revoked(false)
                .build();

        assertThat(token.isValid()).isFalse();
    }

    @Test
    void isValid_revokedAndExpired_returnsFalse() {
        var token = RefreshToken.builder()
                .id(1L)
                .tokenHash("hash")
                .expiresAt(Instant.now().minusSeconds(3600))
                .revoked(true)
                .build();

        assertThat(token.isValid()).isFalse();
    }

    @Test
    void prePersist_setsCreatedAt() {
        var token = RefreshToken.builder()
                .tokenHash("hash")
                .expiresAt(Instant.now().plusSeconds(86400))
                .revoked(false)
                .build();

        assertThat(token.getCreatedAt()).isNull();
    }

    @Test
    void defaultRevoked_isFalse() {
        var token = RefreshToken.builder()
                .tokenHash("hash")
                .expiresAt(Instant.now().plusSeconds(86400))
                .build();

        assertThat(token.isRevoked()).isFalse();
    }

    @Test
    void tokenHashCannotBeNull() {
        var token = RefreshToken.builder()
                .expiresAt(Instant.now().plusSeconds(86400))
                .build();

        assertThat(token.getTokenHash()).isNull();
    }

    @Test
    void expiresAtCannotBeNull() {
        var token = RefreshToken.builder()
                .tokenHash("hash")
                .build();

        assertThat(token.getExpiresAt()).isNull();
    }
}


package com.bioinformatics.authservice.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenResponseTest {

    @Test
    void bearer_createsResponseWithBearerType() {
        var response = TokenResponse.bearer("access", "refresh", 3600);

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.expiresIn()).isEqualTo(3600);
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void serviceBearer_createsResponseWithoutRefreshToken() {
        var response = TokenResponse.serviceBearer("service", 2592000);

        assertThat(response.accessToken()).isEqualTo("service");
        assertThat(response.refreshToken()).isNull();
        assertThat(response.expiresIn()).isEqualTo(2592000);
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void tokenResponseIsSerializable() {
        var response = new TokenResponse("access", "refresh", 3600, "Bearer");

        assertThat(response).isNotNull();
        assertThat(response).isInstanceOf(java.io.Serializable.class);
    }
}


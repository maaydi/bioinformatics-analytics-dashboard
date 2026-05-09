package com.bioinformatics.dashboard.auth.controller;

import com.bioinformatics.dashboard.admin.service.ImportService;
import com.bioinformatics.dashboard.auth.dto.LoginRequest;
import com.bioinformatics.dashboard.auth.dto.RefreshRequest;
import com.bioinformatics.dashboard.auth.dto.TokenResponse;
import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.auth.repository.AppUserRepository;
import com.bioinformatics.dashboard.batch.AsyncUniprotImportJobExecutor;
import com.bioinformatics.dashboard.gene.service.GeneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureRestTestClient
class AuthControllerIntegrationTest {

    @Autowired
    RestTestClient restClient;

    @Autowired
    AppUserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @MockitoBean
    ImportService importService;

    @MockitoBean
    AsyncUniprotImportJobExecutor asyncUniprotImportJobExecutor;

    @MockitoBean
    GeneService geneService;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        var user = AppUser.builder()
                .username("alice")
                .password(passwordEncoder.encode("secret"))
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
    }

    @Test
    void login_shouldReturnAccessAndRefreshTokens() {
        restClient.post()
                .uri("/api/auth/login")
                .body(new LoginRequest("alice", "secret"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody(TokenResponse.class)
                .consumeWith(result -> {
                    TokenResponse body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.accessToken()).isNotBlank();
                    assertThat(body.refreshToken()).isNotBlank();
                    assertThat(body.tokenType()).isEqualTo("Bearer");
                    assertThat(body.expiresIn()).isGreaterThan(0);
                });
    }

    @Test
    void refresh_shouldIssueNewPairWhenRefreshTokenValid() {
        var loginResult = restClient.post()
                .uri("/api/auth/login")
                .body(new LoginRequest("alice", "secret"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenResponse.class)
                .returnResult();

        assert loginResult.getResponseBody() != null;
        var refreshToken = loginResult.getResponseBody().refreshToken();

        restClient.post()
                .uri("/api/auth/refresh")
                .body(new RefreshRequest(refreshToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenResponse.class)
                .consumeWith(result -> {
                    TokenResponse body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.accessToken()).isNotBlank();
                    assertThat(body.refreshToken()).isNotBlank();
                    assertThat(body.refreshToken()).isNotEqualTo(refreshToken);
                });
    }
}
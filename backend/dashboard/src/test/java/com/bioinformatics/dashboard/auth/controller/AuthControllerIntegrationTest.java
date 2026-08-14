package com.bioinformatics.dashboard.auth.controller;

import com.bioinformatics.dashboard.admin.service.ImportService;
import com.bioinformatics.dashboard.auth.dto.*;
import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.auth.repository.AppUserRepository;
import com.bioinformatics.dashboard.exception.ErrorResponse;
import com.bioinformatics.dashboard.job.uniprot.fileloader.AsyncUniprotImportJobExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.rate-limiter.enabled=false")
@ActiveProfiles("test")
@AutoConfigureRestTestClient
@Slf4j
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
    private CacheManager cacheManager;

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


    private @NonNull TokenResponse getLoginResult() {
        var loginResult = restClient.post()
                .uri("/api/auth/login")
                .body(new LoginRequest("alice", "secret"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenResponse.class)
                .returnResult();
        assert loginResult.getResponseBody() != null;
        return loginResult.getResponseBody();

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
        var loginResult = getLoginResult();

        var accessToken = loginResult.accessToken();
        var refreshToken = loginResult.refreshToken();

        restClient.post()
                .uri("/api/auth/refresh")
                .header("Authorization", "Bearer " + accessToken)
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


    @Test
    void logout_shouldInvalidateSecurityContext() {
        var loginResult = getLoginResult();

        var accessToken = loginResult.accessToken();

        restClient.post()
                .uri("/api/auth/logout")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Void.class)
                .consumeWith(result -> {
                    assertThat(result.getResponseBody()).isNull();
                    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
                });
    }

    @Test
    void updatePassword_shouldChangePasswordAndInvalidateSecurityContext() {
        var loginResult = getLoginResult();
        var accessToken = loginResult.accessToken();

        restClient.put()
                .uri("/api/auth/password")
                .header("Authorization", "Bearer " + accessToken)
                .body(new ChangePasswordRequest("Abcd123456789@", "secret"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ChangePasswordResponse.class)
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.success()).isTrue();
                    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
                });
    }

    @Test
    void updatePassword_shouldFailedDueToInvalidPassword() {
        var loginResult = getLoginResult();
        var accessToken = loginResult.accessToken();
        restClient.put()
                .uri("/api/auth/password")
                .header("Authorization", "Bearer " + accessToken)
                .body(new ChangePasswordRequest("Abcdef", "secret"))
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody(ErrorResponse.class)
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.status()).isEqualTo(400);
                    assertThat(body.message()).isEqualTo("newPassword: Password must be at least 12 characters long and contain at least one uppercase letter, one lowercase letter, and one digit");
                });

    }


}
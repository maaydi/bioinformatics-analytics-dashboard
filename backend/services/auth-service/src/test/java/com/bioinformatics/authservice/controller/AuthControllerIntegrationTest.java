package com.bioinformatics.authservice.controller;

import com.bioinformatics.authservice.dto.*;
import com.bioinformatics.authservice.entity.AppUser;
import com.bioinformatics.authservice.repository.AppUserRepository;
import com.bioinformatics.authservice.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import static com.bioinformatics.shared.models.security.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureRestTestClient
class AuthControllerIntegrationTest {

    private static final String AUTH_BASE_URL = "/api/v1/auth";

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        appUserRepository.deleteAll();
        appUserRepository.save(user("alice", "secret", "ROLE_USER"));
        appUserRepository.save(user("admin", "admin123", "ROLE_ADMIN"));
    }

    @Test
    void login_shouldReturnAccessAndRefreshTokens() {
        restClient.post()
                .uri(AUTH_BASE_URL + "/login")
                .body(new LoginRequest("alice", "secret"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody(TokenResponse.class)
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.accessToken()).isNotBlank();
                    assertThat(body.refreshToken()).isNotBlank();
                    assertThat(body.tokenType()).isEqualTo("Bearer");
                    assertThat(body.expiresIn()).isGreaterThan(0);
                });
    }

    @Test
    void refresh_shouldIssueNewPairWhenRefreshTokenValid() {
        var loginResult = login("alice", "secret");

        restClient.post()
                .uri(AUTH_BASE_URL + "/refresh")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginResult.accessToken())
                .body(new RefreshRequest(loginResult.refreshToken()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenResponse.class)
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.accessToken()).isNotBlank();
                    assertThat(body.refreshToken()).isNotBlank();
                    assertThat(body.refreshToken()).isNotEqualTo(loginResult.refreshToken());
                });
    }

    @Test
    void login_shouldNotExposeLegacyPath() {
        restClient.post()
                .uri("/api/auth/login")
                .body(new LoginRequest("alice", "secret"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void login_shouldReturn400WhenPayloadIsInvalid() {
        restClient.post()
                .uri(AUTH_BASE_URL + "/login")
                .body(new LoginRequest("", ""))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.getTitle()).isEqualTo("Bad Request");
                });
    }

    @Test
    void logout_shouldRevokeRefreshTokenAndReturnNoContent() {
        var loginResult = login("alice", "secret");

        restClient.post()
                .uri(AUTH_BASE_URL + "/logout")
                .header(USER_ID_HEADER, "alice")
                .header(USER_ROLE_HEADER, USER_ROLE)
                .exchange()
                .expectStatus().isNoContent();

        restClient.post()
                .uri(AUTH_BASE_URL + "/refresh")
                .body(new RefreshRequest(loginResult.refreshToken()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void updatePassword_shouldChangePasswordAndRevokeExistingRefreshToken() {
        var loginResult = login("alice", "secret");

        restClient.put()
                .uri(AUTH_BASE_URL + "/password")
                .header(USER_ID_HEADER, "alice")
                .header(USER_ROLE_HEADER, USER_ROLE)
                .body(new ChangePasswordRequest("secret", "NewPassword123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("Password changed successfully.");

        restClient.post()
                .uri(AUTH_BASE_URL + "/refresh")
                .body(new RefreshRequest(loginResult.refreshToken()))
                .exchange()
                .expectStatus().isUnauthorized();

        restClient.post()
                .uri(AUTH_BASE_URL + "/login")
                .body(new LoginRequest("alice", "secret"))
                .exchange()
                .expectStatus().isUnauthorized();

        restClient.post()
                .uri(AUTH_BASE_URL + "/login")
                .body(new LoginRequest("alice", "NewPassword123"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void updatePassword_shouldReturn400WhenNewPasswordIsInvalid() {
        var loginResult = login("alice", "secret");

        restClient.put()
                .uri(AUTH_BASE_URL + "/password")
                .header(USER_ID_HEADER, "analytics_user")
                .header(USER_ROLE_HEADER, "USER")
                .body(new ChangePasswordRequest("secret", "Abcdef"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.getProperties()).containsKey("error");
                    assert body.getProperties() != null;
                    assertThat(body.getProperties().get("error").equals("Bad Request"));
                });
    }

    @Test
    void serviceToken_shouldReturnShortLivedTokenForAdmin() {
        var adminLogin = login("admin", "admin123");

        restClient.post()
                .uri(AUTH_BASE_URL + "/service-token")
                .header(USER_ID_HEADER, "admin")
                .header(USER_ROLE_HEADER, "ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenResponse.class)
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.accessToken()).isNotBlank();
                    assertThat(body.refreshToken()).isNull();
                    assertThat(body.expiresIn()).isGreaterThan(0);
                    assertThat(body.tokenType()).isEqualTo("Bearer");
                });
    }

    @Test
    void serviceToken_shouldReturnForbiddenForRegularUser() {
        var userLogin = login("alice", "secret");

        restClient.post()
                .uri(AUTH_BASE_URL + "/service-token")
                .header(USER_ID_HEADER, "analytics_user")
                .header(USER_ROLE_HEADER, "USER")
                .exchange()
                .expectStatus().isForbidden();
    }

    private TokenResponse login(final String username, final String password) {
        var loginResult = restClient.post()
                .uri(AUTH_BASE_URL + "/login")
                .body(new LoginRequest(username, password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenResponse.class)
                .returnResult();

        var body = loginResult.getResponseBody();
        assertThat(body).isNotNull();
        return body;
    }

    private AppUser user(final String username, final String rawPassword, final String role) {
        return AppUser.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();
    }
}


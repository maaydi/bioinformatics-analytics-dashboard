package com.bioinformatics.dashboard.audit;

import com.bioinformatics.dashboard.admin.service.ImportService;
import com.bioinformatics.dashboard.auth.dto.LoginRequest;
import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.auth.repository.AppUserRepository;
import com.bioinformatics.dashboard.job.uniprot.fileloader.AsyncUniprotImportJobExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.rate-limiter.enabled=true")
@ActiveProfiles("test")
@AutoConfigureRestTestClient
class RateLimiterIntegrationTest {

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

    @Test
    void login_shouldReturn429WhenRateLimitExceeded() {
        LoginRequest request = new LoginRequest("alice", "secret");

        // 1. Consume the allowed capacity (5 requests)
        for (int i = 0; i < 5; i++) {
            restClient.post()
                    .uri("/api/auth/login")
                    .body(request)
                    .exchange()
                    .expectStatus().isOk(); // Expecting the first 5 to succeed
        }

        // 2. The 6th request should be blocked
        restClient.post()
                .uri("/api/auth/login")
                .body(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
                .expectBody(String.class) // Assuming you want to read the raw JSON/Text response
                .consumeWith(result -> {
                    String responseBody = result.getResponseBody();
                    assertThat(responseBody).isNotNull();

                    assertThat(responseBody).contains("Rate limit exceeded. Try again later.");
                });
    }
}
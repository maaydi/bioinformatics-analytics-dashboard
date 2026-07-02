package com.bioinformatics.dashboard.audit;

import com.bioinformatics.dashboard.admin.service.ImportService;
import com.bioinformatics.dashboard.audit.entity.AuditLog;
import com.bioinformatics.dashboard.audit.repository.AuditLogRepository;
import com.bioinformatics.dashboard.auth.dto.LoginRequest;
import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.auth.repository.AppUserRepository;
import com.bioinformatics.dashboard.batch.AsyncUniprotImportJobExecutor;
import com.bioinformatics.dashboard.gene.service.GeneService;
import com.bioinformatics.dashboard.model.audit.AuditAction;
import com.bioinformatics.dashboard.model.audit.AuditStatus;
import com.bioinformatics.dashboard.model.audit.AuditTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureRestTestClient
class AuditIntegrationTest {

    @Autowired
    RestTestClient restClient;

    @Autowired
    AuditLogRepository auditLogRepository;

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
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldLogFailedLogin() {
        LoginRequest request = new LoginRequest("nonexistent", "wrongpass");

        restClient.post()
                .uri("/api/auth/login")
                .body(request)
                .exchange()
                .expectStatus().isUnauthorized();

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll();
            assertThat(logs).hasSize(1);
            AuditLog log = logs.getFirst();
            assertThat(log.getAction()).isEqualTo(AuditAction.LOGIN);
            assertThat(log.getStatus()).isEqualTo(AuditStatus.FAILURE);
            assertThat(log.getActorUsername()).isEqualTo("nonexistent");
            assertThat(log.getTarget()).isEqualTo(AuditTarget.AUTH);
        });
    }

    @Test
    void shouldLogSuccessfulLogin() {
        var user = AppUser.builder()
                .username("audittest")
                .password(passwordEncoder.encode("secret"))
                .role("ROLE_USER")
                .build();
        userRepository.save(user);

        LoginRequest request = new LoginRequest("audittest", "secret");

        restClient.post()
                .uri("/api/auth/login")
                .body(request)
                .exchange()
                .expectStatus().isOk();

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            List<AuditLog> logs = auditLogRepository.findAll();
            assertThat(logs).hasSize(1);
            AuditLog log = logs.getFirst();
            assertThat(log.getAction()).isEqualTo(AuditAction.LOGIN);
            assertThat(log.getStatus()).isEqualTo(AuditStatus.SUCCESS);
            assertThat(log.getActorUsername()).isEqualTo("audittest");
            assertThat(log.getTarget()).isEqualTo(AuditTarget.AUTH);
            assertThat(log.getActorId()).isNotNull();
        });
    }
}


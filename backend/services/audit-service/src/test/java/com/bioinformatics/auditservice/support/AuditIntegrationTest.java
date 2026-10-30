package com.bioinformatics.auditservice.support;

import com.bioinformatics.auditservice.AuditServiceApplication;
import com.bioinformatics.common.config.security.GatewayUserAuthenticationFilter;
import com.bioinformatics.common.exception.GlobalExceptionHandler;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(classes = AuditServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({AuditIntegrationTest.Containers.class, GatewayUserAuthenticationFilter.class, GlobalExceptionHandler.class})
public abstract class AuditIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class Containers {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgres() {
            return new PostgreSQLContainer<>("postgres:16-alpine");
        }

        @Bean
        @ServiceConnection(name = "redis")
        GenericContainer<?> redis() {
            return new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
        }
    }
}

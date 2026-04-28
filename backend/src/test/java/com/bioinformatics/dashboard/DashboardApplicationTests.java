package com.bioinformatics.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — verifies the Spring application context loads without errors.
 * Uses Testcontainers via the "test" profile (application-test.yml).
 */
@SpringBootTest
@ActiveProfiles("test")
class DashboardApplicationTests {

    @Test
    void contextLoads() {
        // If this test passes, the Spring context wired correctly.
    }
}

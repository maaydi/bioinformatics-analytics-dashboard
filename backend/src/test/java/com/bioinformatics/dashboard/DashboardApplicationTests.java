package com.bioinformatics.dashboard;

import com.bioinformatics.dashboard.job.uniprot.fileloader.AsyncUniprotImportJobExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test — verifies the Spring application context loads without errors.
 * Uses Testcontainers via the "test" profile (application-test.yml).
 * The tc: JDBC URL in application-test.yml auto-starts PostgreSQL via ContainerDatabaseDriver.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class DashboardApplicationTests {

    @MockitoBean
    AsyncUniprotImportJobExecutor asyncUniprotImportJobExecutor;


    @MockitoBean
    private CacheManager cacheManager;

    @Test
    void contextLoads() {
        // If this test passes, the Spring context wired correctly.
    }
}

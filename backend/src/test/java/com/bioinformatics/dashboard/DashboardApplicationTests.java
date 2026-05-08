package com.bioinformatics.dashboard;

import com.bioinformatics.dashboard.batch.AsyncUniprotImportJobExecutor;
import com.bioinformatics.dashboard.gene.service.GeneService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
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


    // GeneService has no implementation yet (pre-existing stub); mock it so context loads.
    @MockitoBean
    GeneService geneService;


    @Test
    void contextLoads() {
        // If this test passes, the Spring context wired correctly.
    }
}

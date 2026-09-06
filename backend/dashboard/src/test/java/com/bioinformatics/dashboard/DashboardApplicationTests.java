package com.bioinformatics.dashboard;

import com.bioinformatics.common.providers.uniprotkb.service.UniprotKbPaginationCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Smoke test — verifies the Spring application context loads without errors.
 * Uses Testcontainers via the "test" profile (application-test.yml).
 * The tc: JDBC URL in application-test.yml auto-starts PostgreSQL via ContainerDatabaseDriver.
 */
@Testcontainers
@SpringBootTest
class DashboardApplicationTests {

    @MockitoBean
    UniprotKbPaginationCacheService uniprotKbPaginationCacheService;

    @MockitoBean
    private CacheManager cacheManager;

    @Test
    void contextLoads() {
        // If this test passes, the Spring context wired correctly.
    }
}

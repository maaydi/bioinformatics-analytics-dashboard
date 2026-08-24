package com.bioinformatics.analyticsservice.db.routing;

import com.bioinformatics.analyticsservice.AnalyticsServiceApplication;
import com.bioinformatics.common.config.datasource.RoutingDataSourceConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that analytics-service acquires ZERO *additional* PRIMARY
 * connections while serving read-only analytics queries.
 * <p>
 * Note: Spring/Hibernate may hold 1 PRIMARY connection from EMF bootstrap
 * (schema validation, dialect detection). That is startup overhead, not an
 * analytics query. We baseline it and assert no growth.
 */
@SpringBootTest(classes = AnalyticsServiceApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        /* ── PRIMARY: must not grow during test ── */
        "common.datasource.primary-url=jdbc:tc:postgresql:16:///primary?TC_INITSCRIPT=db/init-primary.sql",
        "common.datasource.primary-username=bio_user",
        "common.datasource.primary-password=bio_password",

        /* ── REPLICA: where all analytics traffic lands ── */
        "common.datasource.replica-url=jdbc:tc:postgresql:16:///replica?TC_INITSCRIPT=db/init-replica.sql",
        "common.datasource.replica-username=bio_user",
        "common.datasource.replica-password=bio_password",

        "common.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",

        /* ── Read-heavy pool sizing ── */
        "common.datasource.pool.max-size=50",
        "common.datasource.pool.min-idle=0",
        "common.datasource.pool.connection-timeout-ms=30000",

        /* ── Minimize Hibernate bootstrap noise (optional but helpful) ── */
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never"
})
@Transactional(readOnly = true)
class ReadReplicaRoutingTest {

    @Autowired
    private DataSource routingDataSource;

    private HikariDataSource primaryPool;
    private HikariDataSource replicaPool;

    /**
     * Connections already held by PRIMARY after Spring context startup.
     */
    private int primaryBaseline;

    @BeforeEach
    void extractPoolsAndRecordBaseline() throws Exception {
        var proxy = (LazyConnectionDataSourceProxy) routingDataSource;
        var routingDS = (AbstractRoutingDataSource) proxy.getTargetDataSource();

        var resolvedField = AbstractRoutingDataSource.class.getDeclaredField("resolvedDataSources");
        resolvedField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var resolved = (Map<Object, DataSource>) resolvedField.get(routingDS);

        primaryPool = (HikariDataSource) resolved.get(RoutingDataSourceConfig.DataSourceType.PRIMARY);
        replicaPool = (HikariDataSource) resolved.get(RoutingDataSourceConfig.DataSourceType.REPLICA);

        primaryBaseline = totalConnections(primaryPool);
    }

    @Test
    @DisplayName("Analytics query routes to REPLICA and does not touch PRIMARY")
    void analyticsQuery_zeroNewPrimaryConnections() {
        // Given: whatever startup connections exist on PRIMARY
        assertThat(totalConnections(primaryPool))
                .as("PRIMARY startup baseline (Hibernate EMF init)")
                .isLessThanOrEqualTo(1); // usually 0 or 1; fails loudly if it spikes

        // When: execute an analytics-style query inside the read-only tx
        var tpl = new JdbcTemplate(routingDataSource);
        String result = tpl.queryForObject(
                "SELECT src FROM routing_test WHERE id = 1", String.class);

        // Then 1: functional — data came from the replica database
        assertThat(result)
                .as("Query should read from REPLICA container")
                .isEqualTo("REPLICA_DB");

        // Then 2: structural — PRIMARY did not grow
        assertThat(totalConnections(primaryPool))
                .as("PRIMARY connections must not increase during analytics query")
                .isEqualTo(primaryBaseline);

        // Then 3: structural — REPLICA actually served the request
        assertThat(totalConnections(replicaPool))
                .as("REPLICA pool should have active connections")
                .isPositive();
    }

    @Test
    @DisplayName("Sequential read-only queries continue to avoid PRIMARY")
    void sequentialQueries_noPrimaryGrowth() {
        int before = totalConnections(primaryPool);

        var tpl = new JdbcTemplate(routingDataSource);
        tpl.queryForObject("SELECT src FROM routing_test WHERE id = 1", String.class);
        tpl.queryForObject("SELECT src FROM routing_test WHERE id = 1", String.class);
        tpl.queryForObject("SELECT src FROM routing_test WHERE id = 1", String.class);

        assertThat(totalConnections(primaryPool))
                .as("PRIMARY connections must stay flat across multiple analytics queries")
                .isEqualTo(before);
    }

    @Test
    @DisplayName("REPLICA pool is sized for read-heavy load (max 50)")
    void replicaPool_maxSizeFifty() {
        assertThat(replicaPool.getMaximumPoolSize())
                .as("REPLICA pool max-size should be 50")
                .isEqualTo(50);
    }

    private int totalConnections(HikariDataSource pool) {
        if (pool == null) return 0;
        var mxBean = pool.getHikariPoolMXBean();
        return (mxBean != null) ? mxBean.getTotalConnections() : 0;
    }
}
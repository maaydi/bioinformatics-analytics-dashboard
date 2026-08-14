package com.bioinformatics.common.config.datasource;


import com.bioinformatics.common.config.CommonProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Configures an {@link AbstractRoutingDataSource} that routes read-only
 * transactions to the REPLICA and write transactions to the PRIMARY.
 * <p>Usage in a service:
 * <pre>{@code
 *   @Transactional(readOnly = true)
 *   public List<Gene> findAll() { ... }   // → REPLICA
 *
 *   @Transactional
 *   public Gene save(Gene g) { ... }      // → PRIMARY
 * }</pre>
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(DataSource.class)
@ConditionalOnProperty(prefix = "common.datasource", name = "primary-url")
@EnableConfigurationProperties(CommonProperties.class)
public class RoutingDataSourceConfig {

    private final CommonProperties commonProperties;

    @Bean
    @Primary
    public DataSource routingDataSource() {
        var routingDataSource = new AbstractRoutingDataSource() {
            @Override
            protected Object determineCurrentLookupKey() {
                return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                        ? DataSourceType.REPLICA
                        : DataSourceType.PRIMARY;
            }
        };

        var dsProps = commonProperties.datasource();
        var pool = dsProps.pool();

        var primary = createDataSource(
                dsProps.primaryUrl(),
                dsProps.primaryUsername(),
                dsProps.primaryPassword(),
                pool
        );
        var replica = createDataSource(
                dsProps.replicaUrl(),
                dsProps.replicaUsername(),
                dsProps.replicaPassword(),
                pool
        );

        Map<Object, Object> targets = new HashMap<>();
        targets.put(DataSourceType.PRIMARY, primary);
        targets.put(DataSourceType.REPLICA, replica);

        routingDataSource.setTargetDataSources(targets);
        routingDataSource.setDefaultTargetDataSource(primary);

        return routingDataSource;
    }

    private DataSource createDataSource(String url, String username, String password,
                                        CommonProperties.DataSource.Pool pool) {
        var config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(commonProperties.datasource().driverClassName());
        config.setMaximumPoolSize(pool.maxSize());
        config.setMinimumIdle(pool.minIdle());
        config.setConnectionTimeout(pool.connectionTimeoutMs());
        config.setPoolName("CommonPool-" + (url.contains("primary") ? "PRIMARY" : "REPLICA"));
        return new HikariDataSource(config);
    }

    public enum DataSourceType {
        PRIMARY, REPLICA
    }
}

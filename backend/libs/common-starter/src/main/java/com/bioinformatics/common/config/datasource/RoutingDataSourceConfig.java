package com.bioinformatics.common.config.datasource;

import com.bioinformatics.common.config.CommonProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.flyway.autoconfigure.FlywayDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.HashMap;

@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(DataSource.class)
@ConditionalOnProperty(prefix = "common.datasource", name = "primary-url")
@EnableConfigurationProperties(CommonProperties.class)
@Slf4j
public class RoutingDataSourceConfig {

    private final CommonProperties commonProperties;

    @Bean
    @FlywayDataSource
    public DataSource primaryDataSource() {
        log.debug("Creating PRIMARY datasource");
        var dsProps = commonProperties.datasource();
        return createDataSource(
                DataSourceType.PRIMARY,
                dsProps.primaryUrl(),
                dsProps.primaryUsername(),
                dsProps.primaryPassword(),
                dsProps.pool()
        );
    }

    @Bean
    public DataSource replicaDataSource() {
        log.debug("Creating REPLICA datasource");
        var dsProps = commonProperties.datasource();
        return createDataSource(
                DataSourceType.REPLICA,
                dsProps.replicaUrl(),
                dsProps.replicaUsername(),
                dsProps.replicaPassword(),
                dsProps.pool()
        );
    }

    @Bean
    @Primary
    public DataSource routingDataSource(DataSource primaryDataSource, DataSource replicaDataSource) {
        log.debug("Creating routing datasource");
        var routingDataSource = new AbstractRoutingDataSource() {
            @Override
            protected Object determineCurrentLookupKey() {
                return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                        ? DataSourceType.REPLICA
                        : DataSourceType.PRIMARY;
            }
        };

        var targets = new HashMap<Object, Object>();
        targets.put(DataSourceType.PRIMARY, primaryDataSource);
        targets.put(DataSourceType.REPLICA, replicaDataSource);

        routingDataSource.setTargetDataSources(targets);
        routingDataSource.setDefaultTargetDataSource(primaryDataSource);

        routingDataSource.afterPropertiesSet();
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }

    private DataSource createDataSource(DataSourceType type, String url, String username, String password,
                                        CommonProperties.DataSource.Pool pool) {
        var config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(commonProperties.datasource().driverClassName());
        config.setMaximumPoolSize(pool.maxSize());
        config.setMinimumIdle(pool.minIdle());
        config.setConnectionTimeout(pool.connectionTimeoutMs());
        config.setPoolName("CommonPool-" + type.name());
        return new HikariDataSource(config);
    }

    public enum DataSourceType {
        PRIMARY, REPLICA
    }
}
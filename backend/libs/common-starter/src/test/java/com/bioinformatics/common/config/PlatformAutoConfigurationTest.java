package com.bioinformatics.common.config;

import com.bioinformatics.common.audit.config.AuditPublisherConfig;
import com.bioinformatics.common.audit.service.AuditEventPublisher;
import com.bioinformatics.common.config.cache.CommonCacheConfig;
import com.bioinformatics.common.config.correlation.CorrelationAutoConfiguration;
import com.bioinformatics.common.config.correlation.FeignCorrelationAutoConfiguration;
import com.bioinformatics.common.config.datasource.RoutingDataSourceConfig;
import com.bioinformatics.common.config.jwt.JwtDecoderConfig;
import com.bioinformatics.common.config.kafka.KafkaConsumerConfig;
import com.bioinformatics.common.config.kafka.KafkaProducerConfig;
import com.bioinformatics.common.config.resilience.Resilience4jConfig;
import com.bioinformatics.common.config.security.CommonSecurityConfig;
import com.bioinformatics.common.config.tracing.TracingConfig;
import com.bioinformatics.common.config.web.WebClientConfig;
import com.bioinformatics.common.config.web.WebMvcConfig;
import com.bioinformatics.common.gene.service.ProteinEntryService;
import com.bioinformatics.common.providers.ProviderFilter;
import com.bioinformatics.common.ratelimit.config.RateLimitConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import javax.sql.DataSource;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PlatformAutoConfigurationTest {
    private static final Class<?>[] FEATURES = {
            CommonAutoConfiguration.class, LegacyCatalogAutoConfiguration.class, LegacyUniprotAutoConfiguration.class,
            CommonCacheConfig.class, RoutingDataSourceConfig.class, JwtDecoderConfig.class,
            KafkaProducerConfig.class, KafkaConsumerConfig.class, Resilience4jConfig.class,
            CommonSecurityConfig.class, TracingConfig.class, WebClientConfig.class, WebMvcConfig.class,
            CorrelationAutoConfiguration.class, FeignCorrelationAutoConfiguration.class,
            AuditPublisherConfig.class, RateLimitConfig.class
    };

    @Test
    void everyFeatureIsRegisteredExplicitly() {
        var imports = ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()).getCandidates();
        assertThat(imports).contains(Stream.of(FEATURES).map(Class::getName).toArray(String[]::new));
    }

    @Test
    void masterSwitchDisablesEveryFeature() {
        new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(FEATURES))
                .withPropertyValues("common.enabled=false", "app.audit.enabled=true",
                        "app.legacy-catalog.enabled=true", "common.kafka.bootstrap-servers=localhost:9092",
                        "common.jwt.secret=01234567890123456789012345678901")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    for (var feature : FEATURES) {
                        assertThat(context).doesNotHaveBean(feature);
                    }
                });
    }

    @Test
    void lightweightConsumerDoesNotNeedOptionalInfrastructure() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(FEATURES))
                .withClassLoader(new FilteredClassLoader("org.springframework.data", "jakarta.persistence",
                        "org.springframework.jdbc", "org.springframework.kafka", "org.springframework.web",
                        "org.springframework.security", "brave", "io.github.resilience4j", "io.github.bucket4j",
                        "feign", "org.flywaydb", "org.springframework.batch"))
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(CommonProperties.class)
                        .doesNotHaveBean(ProteinEntryService.class).doesNotHaveBean(AuditEventPublisher.class));
    }

    @ParameterizedTest
    @MethodSource("disabledFeatures")
    void featureSwitchDisablesItsConfiguration(Feature feature) {
        new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(feature.type()))
                .withPropertyValues(feature.property() + "=false", "common.kafka.bootstrap-servers=localhost:9092",
                        "common.jwt.secret=01234567890123456789012345678901")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(feature.type()));
    }

    static Stream<Feature> disabledFeatures() {
        return Stream.of(
                new Feature(LegacyCatalogAutoConfiguration.class, "app.legacy-catalog.enabled"),
                new Feature(LegacyUniprotAutoConfiguration.class, "app.legacy-catalog.enabled"),
                new Feature(CommonCacheConfig.class, "common.cache.enabled"),
                new Feature(RoutingDataSourceConfig.class, "common.datasource.enabled"),
                new Feature(KafkaProducerConfig.class, "common.kafka.enabled"),
                new Feature(KafkaConsumerConfig.class, "common.kafka.enabled"),
                new Feature(Resilience4jConfig.class, "common.resilience4j.enabled"),
                new Feature(CommonSecurityConfig.class, "common.security.enabled"),
                new Feature(TracingConfig.class, "common.tracing.enabled"),
                new Feature(WebClientConfig.class, "common.web-client.enabled"),
                new Feature(WebMvcConfig.class, "common.web.enabled"),
                new Feature(CorrelationAutoConfiguration.class, "common.correlation.enabled"),
                new Feature(FeignCorrelationAutoConfiguration.class, "common.correlation.enabled"),
                new Feature(AuditPublisherConfig.class, "app.audit.enabled"),
                new Feature(RateLimitConfig.class, "app.rate-limiter.enabled"));
    }

    @Test
    void catalogAndAuditAreOffUnlessExplicitlyRequested() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(FEATURES))
                .withPropertyValues("common.tracing.enabled=false", "common.resilience4j.enabled=false",
                        "common.web-client.enabled=false")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(ProteinEntryService.class)
                        .doesNotHaveBean(LegacyUniprotAutoConfiguration.class)
                        .doesNotHaveBean(AuditEventPublisher.class));
    }

    @Test
    void auditProducerStartsWithoutPersistenceInfrastructure() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(AuditPublisherConfig.class))
                .withClassLoader(new FilteredClassLoader("jakarta.persistence", "org.springframework.data",
                        "org.springframework.jdbc", "org.flywaydb", "org.springframework.batch", "jakarta.servlet"))
                .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
                .withPropertyValues("app.audit.enabled=true", "spring.application.name=producer")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(AuditEventPublisher.class)
                        .doesNotHaveBean(javax.sql.DataSource.class));
    }

    @Test
    void auditOptInWithoutTransportFailsExplicitly() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(AuditPublisherConfig.class))
                .withPropertyValues("app.audit.enabled=true", "spring.application.name=producer")
                .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @CsvSource({"core-size,0", "max-size,0", "queue-capacity,0", "queue-capacity,-1"})
    void invalidAuditPoolIsRejected(String key, String value) {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(AuditPublisherConfig.class))
                .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
                .withPropertyValues("app.audit.enabled=true", "spring.application.name=producer",
                        "app.audit-pool." + key + "=" + value)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void jwtRequiresItsConfiguredSecret() {
        var runner = new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(JwtDecoderConfig.class));
        runner.run(context -> assertThat(context).doesNotHaveBean(JwtDecoder.class));
        runner.withPropertyValues("common.jwt.secret=01234567890123456789012345678901")
                .run(context -> assertThat(context).hasSingleBean(JwtDecoder.class));
    }

    record Feature(Class<?> type, String property) {
    }

    @Test
    void mvcProviderRoutingDoesNotRequireLegacyUniprotOrJpa() {
        new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(WebMvcConfig.class))
                .withClassLoader(new FilteredClassLoader("jakarta.persistence", "org.springframework.data",
                        "org.springframework.retry"))
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(ProviderFilter.class));
    }

    @ParameterizedTest
    @MethodSource("enabledFeatures")
    void availableFeaturesStartWhenEnabled(Feature feature) {
        new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(CommonAutoConfiguration.class, feature.type()))
                .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class))
                .withBean(JwtDecoder.class, () -> mock(JwtDecoder.class))
                .withBean(CacheManager.class, NoOpCacheManager::new)
                .withPropertyValues(feature.property() + "=true", "common.kafka.bootstrap-servers=localhost:9092",
                        "common.tracing.sampling-rate=0")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(feature.type()));
    }

    static Stream<Feature> enabledFeatures() {
        return disabledFeatures().filter(feature -> feature.type() != LegacyCatalogAutoConfiguration.class
                && feature.type() != RoutingDataSourceConfig.class
                && feature.type() != AuditPublisherConfig.class);
    }

    @Test
    void routingIsAbsentWithoutConnectionSettingsAndSupportsExistingPools() {
        var runner = new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(RoutingDataSourceConfig.class));
        runner.run(context -> assertThat(context).doesNotHaveBean(DataSource.class));
        runner.withBean("primaryDataSource", DataSource.class, () -> mock(DataSource.class))
                .withBean("replicaDataSource", DataSource.class, () -> mock(DataSource.class))
                .withPropertyValues("common.datasource.primary-url=jdbc:postgresql://unused/catalog")
                .run(context -> assertThat(context).hasNotFailed().hasBean("routingDataSource"));
    }
}

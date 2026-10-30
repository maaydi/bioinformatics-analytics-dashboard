package com.bioinformatics.common.audit;

import com.bioinformatics.common.audit.aspect.AuditAspect;
import com.bioinformatics.common.audit.config.AuditPublisherConfig;
import com.bioinformatics.common.audit.service.AuditContextFilter;
import com.bioinformatics.common.audit.service.AuditEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AuditPublisherConfigTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void activatesWithKafkaAndUsesDefaultPoolWithoutServletContext() {
        new ApplicationContextRunner().withUserConfiguration(AuditPublisherConfig.class)
                .withBean(KafkaTemplate.class, () -> kafkaTemplate)
                .withPropertyValues("spring.application.name=test-service", "common.kafka.bootstrap-servers=localhost:9092")
                .run(context -> {
                    assertThat(context).hasSingleBean(AuditEventPublisher.class).hasSingleBean(AuditAspect.class)
                            .doesNotHaveBean(AuditContextFilter.class);
                    var executor = context.getBean("auditPublisherExecutor", ThreadPoolTaskExecutor.class);
                    assertThat(executor.getCorePoolSize()).isEqualTo(1);
                    assertThat(executor.getMaxPoolSize()).isEqualTo(10);
                    assertThat(executor.getQueueCapacity()).isEqualTo(25);
                    assertThat(executor.getThreadNamePrefix()).isEqualTo("audit-");
                });
    }

    @Test
    void staysInactiveWithoutKafkaConfiguration() {
        new ApplicationContextRunner().withUserConfiguration(AuditPublisherConfig.class)
                .run(context -> assertThat(context).doesNotHaveBean(AuditEventPublisher.class)
                        .doesNotHaveBean(AuditAspect.class).doesNotHaveBean(AuditContextFilter.class));
    }

    @Test
    void bindsExistingPoolPropertiesAndRegistersServletFilter() {
        new WebApplicationContextRunner().withUserConfiguration(AuditPublisherConfig.class)
                .withBean(KafkaTemplate.class, () -> kafkaTemplate)
                .withPropertyValues("spring.application.name=test-service", "common.kafka.bootstrap-servers=localhost:9092",
                        "app.audit-pool.core-size=2", "app.audit-pool.max-size=4",
                        "app.audit-pool.queue-capacity=8", "app.audit-pool.thread-name-prefix=test-audit-")
                .run(context -> {
                    assertThat(context).hasSingleBean(AuditContextFilter.class);
                    var executor = context.getBean("auditPublisherExecutor", ThreadPoolTaskExecutor.class);
                    assertThat(executor.getCorePoolSize()).isEqualTo(2);
                    assertThat(executor.getMaxPoolSize()).isEqualTo(4);
                    assertThat(executor.getQueueCapacity()).isEqualTo(8);
                    assertThat(executor.getThreadNamePrefix()).isEqualTo("test-audit-");
                });
    }
}

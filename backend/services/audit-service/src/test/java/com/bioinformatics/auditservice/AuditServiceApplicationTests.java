package com.bioinformatics.auditservice;

import com.bioinformatics.auditservice.config.ApplicationProperties;
import com.bioinformatics.auditservice.controller.AuditLogController;
import com.bioinformatics.auditservice.service.AuditConsumer;
import com.bioinformatics.auditservice.support.AuditIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;

import static org.assertj.core.api.Assertions.assertThat;

class AuditServiceApplicationTests extends AuditIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context.getBean(AuditLogController.class)).isNotNull();
        assertThat(context.getBean(AuditConsumer.class)).isNotNull();
        assertThat(context.getBeansOfType(KafkaListenerEndpointRegistry.class)).isEmpty();
        var pool = context.getBean(ApplicationProperties.class).auditPool();
        assertThat(pool.coreSize()).isEqualTo(1);
        assertThat(pool.maxSize()).isEqualTo(10);
        assertThat(pool.queueCapacity()).isEqualTo(25);
        assertThat(pool.threadNamePrefix()).isEqualTo("audit-");
    }
}

package com.bioinformatics.common.audit.config;

import com.bioinformatics.common.audit.aspect.AuditAspect;
import com.bioinformatics.common.audit.service.AuditContextFilter;
import com.bioinformatics.common.audit.service.AuditEventPublisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "common.kafka", name = "bootstrap-servers")
@EnableConfigurationProperties(AuditPublisherProperties.class)
@EnableAspectJAutoProxy
public class AuditPublisherConfig {

    @Bean
    public ThreadPoolTaskExecutor auditPublisherExecutor(AuditPublisherProperties properties) {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.coreSize());
        executor.setMaxPoolSize(properties.maxSize());
        executor.setQueueCapacity(properties.queueCapacity());
        executor.setThreadNamePrefix(properties.threadNamePrefix());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    @Bean
    public AuditEventPublisher auditEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                                 @Qualifier("auditPublisherExecutor") ThreadPoolTaskExecutor executor,
                                                 @Value("${spring.application.name}") String sourceService) {
        return new AuditEventPublisher(kafkaTemplate, executor, sourceService);
    }

    @Bean
    public AuditAspect auditAspect(AuditEventPublisher publisher) {
        return new AuditAspect(publisher);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public AuditContextFilter auditContextFilter() {
        return new AuditContextFilter();
    }
}

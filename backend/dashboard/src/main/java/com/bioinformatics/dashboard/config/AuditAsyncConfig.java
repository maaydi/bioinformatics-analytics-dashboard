package com.bioinformatics.dashboard.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configures the asynchronous executor used to persist audit events outside the request thread.
 *
 * <p>This executor isolates audit persistence from user-facing latency and keeps audit writes from blocking the main
 * request pipeline. It is used by {@link com.bioinformatics.dashboard.audit.service.AuditService} when recording action logs.</p>
 */
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AuditAsyncConfig {

    private final AppProperties appProperties;

    /**
     * Creates the dedicated executor for asynchronous audit writes.
     *
     * @return configured thread pool executor
     */
    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        var conf = appProperties.getAuditPool();
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(conf.getCoreSize());
        executor.setMaxPoolSize(conf.getMaxSize());
        executor.setQueueCapacity(conf.getQueueCapacity());
        executor.setThreadNamePrefix(conf.getThreadNamePrefix());
        executor.initialize();
        return executor;
    }
}

package com.bioinformatics.dashboard.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AuditAsyncConfig {

    private final AppProperties appProperties;


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

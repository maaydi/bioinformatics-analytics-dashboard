package com.bioinformatics.auditservice;

import com.bioinformatics.auditservice.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableDiscoveryClient
@EntityScan(basePackages = {
        "com.bioinformatics.auditservice",
})
@EnableJpaRepositories(basePackages = {
        "com.bioinformatics.auditservice",
})
@EnableConfigurationProperties(ApplicationProperties.class)
public class AuditServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }

}

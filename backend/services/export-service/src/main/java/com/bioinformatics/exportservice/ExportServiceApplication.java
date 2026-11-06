package com.bioinformatics.exportservice;

import com.bioinformatics.exportservice.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableConfigurationProperties(ApplicationProperties.class)
@EnableCaching
@EnableScheduling
@EnableJpaRepositories(basePackageClasses = ExportServiceApplication.class)
@EntityScan(basePackageClasses = ExportServiceApplication.class)
public class ExportServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(ExportServiceApplication.class, args);
    }

}

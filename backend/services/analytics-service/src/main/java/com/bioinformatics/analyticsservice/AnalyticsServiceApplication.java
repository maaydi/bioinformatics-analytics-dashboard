package com.bioinformatics.analyticsservice;

import com.bioinformatics.analyticsservice.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableDiscoveryClient
@EntityScan(basePackages = {
        "com.bioinformatics.analyticsservice",
        "com.bioinformatics.common.gene.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.bioinformatics.analyticsservice",
        "com.bioinformatics.common"
})
@EnableConfigurationProperties(ApplicationProperties.class)
public class AnalyticsServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }

}

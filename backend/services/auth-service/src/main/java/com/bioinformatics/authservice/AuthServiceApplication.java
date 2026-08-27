package com.bioinformatics.authservice;

import com.bioinformatics.authservice.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(ApplicationProperties.class)
@EntityScan(basePackages = {
        "com.bioinformatics.authservice",
        "com.bioinformatics.common.gene.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.bioinformatics.authservice",
        "com.bioinformatics.common"
})
public class AuthServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}

package com.bioinformatics.importservice;

import com.bioinformatics.importservice.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableConfigurationProperties(ApplicationProperties.class)
@EnableCaching
@ComponentScan(basePackages = {
        "com.bioinformatics.importservice",
        "com.bioinformatics.common"
})
@EnableJpaRepositories(basePackages = {
        "com.bioinformatics.importservice",
        "com.bioinformatics.common"
})
@EntityScan(basePackages = {
        "com.bioinformatics.importservice",
        "com.bioinformatics.common"
})
public class ImportServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(ImportServiceApplication.class, args);
    }

}

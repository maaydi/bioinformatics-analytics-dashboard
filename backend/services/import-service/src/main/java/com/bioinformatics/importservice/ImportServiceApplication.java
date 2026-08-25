package com.bioinformatics.importservice;

import com.bioinformatics.importservice.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableConfigurationProperties(ApplicationProperties.class)
public class ImportServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(ImportServiceApplication.class, args);
    }

}

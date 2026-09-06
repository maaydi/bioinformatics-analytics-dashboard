package com.bioinformatics.apigateway;

import com.bioinformatics.apigateway.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(ApplicationProperties.class)
public class ApiGatewayApplication {

    static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

}

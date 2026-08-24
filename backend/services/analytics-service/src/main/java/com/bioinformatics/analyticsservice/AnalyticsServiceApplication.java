package com.bioinformatics.analyticsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AnalyticsServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }

}

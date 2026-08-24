package com.bioinformatics.analyticsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EntityScan(basePackages = {
        "com.bioinformatics.analyticsservice",
        "com.bioinformatics.common.gene.entity"
})
public class AnalyticsServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }

}

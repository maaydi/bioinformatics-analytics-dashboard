package com.bioinformatics.importservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ImportServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(ImportServiceApplication.class, args);
    }

}

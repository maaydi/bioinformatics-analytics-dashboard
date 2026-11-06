package com.bioinformatics.geneservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableDiscoveryClient
@EntityScan(basePackages = {
        "com.bioinformatics.geneservice",
        "com.bioinformatics.common.gene.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.bioinformatics.geneservice",
        "com.bioinformatics.common"
})
public class GeneServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(GeneServiceApplication.class, args);
    }

}

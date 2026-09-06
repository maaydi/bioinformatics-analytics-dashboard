package com.bioinformatics.common.config;


import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Entry-point autoconfiguration for the {@code common-starter}.
 * <p>Enabled by default via {@code common.enabled=true} (or missing property).
 * Scans all sub-packages so that conditional beans are picked up automatically.
 */
@AutoConfiguration
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@EnableConfigurationProperties(CommonProperties.class)
@ConditionalOnProperty(prefix = "common", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.bioinformatics.common")
public class CommonAutoConfiguration {


}
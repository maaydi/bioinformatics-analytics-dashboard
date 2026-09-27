package com.bioinformatics.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Entry-point auto-configuration for the {@code common-starter}.
 *
 * <p>Enabled by default via {@code common.enabled=true} (or missing property), this configuration
 * scans the {@code com.bioinformatics.common} package and loads the shared infrastructure beans used
 * across services.
 *
 * <p>Purpose:
 * <ul>
 *   <li>Register common configuration properties</li>
 *   <li>Enable package scanning for shared components</li>
 *   <li>Bootstrap datasource / common infra wiring before database autoconfiguration</li>
 * </ul>
 */
@AutoConfiguration
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@EnableConfigurationProperties(CommonProperties.class)
@ConditionalOnProperty(prefix = "common", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.bioinformatics.common")
public class CommonAutoConfiguration {


}
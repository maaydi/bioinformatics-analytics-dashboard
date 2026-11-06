package com.bioinformatics.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Entry-point auto-configuration for the {@code common-starter}.
 *
 * Registers shared settings; feature configurations are imported independently without package scanning.
 */
@AutoConfiguration
@EnableConfigurationProperties(CommonProperties.class)
@ConditionalOnProperty(prefix = "common", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CommonAutoConfiguration {


}
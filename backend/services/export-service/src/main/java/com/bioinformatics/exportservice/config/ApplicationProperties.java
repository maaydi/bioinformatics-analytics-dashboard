package com.bioinformatics.exportservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record ApplicationProperties(@DefaultValue Export export) {
    public record Export(@DefaultValue Csv csv, @DefaultValue String tempDir) {
    }

    public record Csv ( @DefaultValue("100000") int maxRows){
    }
}

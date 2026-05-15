package com.bioinformatics.dashboard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Batch batch = new Batch();
    private final ImportConfig importConfig = new ImportConfig();
    private final Export export = new Export();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long accessTokenExpirySeconds;
        private long refreshTokenExpirySeconds;
    }

    @Getter
    @Setter
    public static class Batch {
        private int chunkSize;

    }

    @Getter
    @Setter
    public static class ImportConfig {
        private String tempDir;
        private List<String> extensions;
        private ThreadPoolSettings pool;
    }

    @Getter
    @Setter
    public static class ThreadPoolSettings {
        private int coreSize;
        private int maxSize;
        private int queueCapacity;
        private String threadNamePrefix;
    }

    @Getter
    @Setter
    public static class Export {
        private Csv csv;
    }

    @Getter
    @Setter
    public static class Csv {
        private int maxRows;
    }
}

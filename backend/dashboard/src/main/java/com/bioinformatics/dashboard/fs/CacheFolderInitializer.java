package com.bioinformatics.dashboard.fs;

import com.bioinformatics.dashboard.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Slf4j
@RequiredArgsConstructor
public class CacheFolderInitializer implements CommandLineRunner {

    private final AppProperties appProperties;

    @Override
    public void run(String @NonNull ... args) throws Exception {
        var cacheFolder = new File(appProperties.getImportConfig().getTempDir());
        var created = cacheFolder.mkdirs();
        log.info("Cache Folder {}", created ? "successfully created " : " already exists, skip creation.");
    }
}

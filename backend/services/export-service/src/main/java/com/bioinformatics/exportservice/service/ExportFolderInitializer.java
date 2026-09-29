package com.bioinformatics.exportservice.service;

import com.bioinformatics.exportservice.config.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExportFolderInitializer implements CommandLineRunner {

    private final ApplicationProperties properties;

    @Override
    public void run(String @NonNull ... args) throws Exception {
        var cacheFolder = new File(properties.export().tempDir());
        var created = cacheFolder.mkdirs();
        log.info("Export Temp Folder {}", created ? "successfully created " : " already exists, skip creation.");
    }
}

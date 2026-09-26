package com.bioinformatics.exportservice.service;

import com.bioinformatics.exportservice.assembler.DelimitedSegmentAssembler;
import com.bioinformatics.exportservice.assembler.ExcelSegmentAssembler;
import com.bioinformatics.exportservice.assembler.JsonSegmentAssembler;
import com.bioinformatics.exportservice.assembler.SegmentAssemblerRegistry;
import com.bioinformatics.exportservice.config.ApplicationProperties;
import com.bioinformatics.exportservice.dto.DefaultExportFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportFileStorageServiceTest {

    @Mock
    private ApplicationProperties appProperties;

    private static ApplicationProperties.Export exportConfig(Path tempDir) {
        return new ApplicationProperties.Export(10000, tempDir.toString(),
                new ApplicationProperties.ThreadPoolSettings(1, 5, 10, "Test-Storage"),
                new ApplicationProperties.Batch(100), new ApplicationProperties.Cleanup(1L));
    }

    DefaultExportFileStorageService createService() {
        var registry = new SegmentAssemblerRegistry(List.of(
                new DelimitedSegmentAssembler(),
                new JsonSegmentAssembler(),
                new ExcelSegmentAssembler()
        ));

        return new DefaultExportFileStorageService(appProperties, registry);
    }

    @AfterEach
    void cleanup() {
        // TempDir is cleaned automatically by JUnit
    }

    @Test
    void createPipelineDirectory_createsExpectedStructure(@TempDir Path tempDir) throws IOException {
        when(appProperties.export()).thenReturn(exportConfig(tempDir));
        var svc = createService();
        Path dir = svc.createPipelineDirectory("user", 123L);
        assertThat(Files.exists(dir)).isTrue();
        assertThat(Files.exists(dir.resolve("segments"))).isTrue();
    }

    @Test
    void assembleSegments_concatenatesCsvFiles(@TempDir Path tempDir) throws IOException {
        when(appProperties.export()).thenReturn(exportConfig(tempDir));
        var svc = createService();
        svc.createPipelineDirectory("user", 2L);
        Path segDir = tempDir.resolve("user").resolve("2").resolve("segments");
        Files.writeString(segDir.resolve("segment_00001.csv"), "id,name\n1,alice\n", StandardCharsets.UTF_8);
        Files.writeString(segDir.resolve("segment_00002.csv"), "id,name\n2,bob\n", StandardCharsets.UTF_8);

        Path finalFile = svc.assembleSegments("user", 2L, DefaultExportFormat.CSV);
        String content = Files.readString(finalFile, StandardCharsets.UTF_8);

        assertThat(content).contains("id,name");
        assertThat(content).contains("1,alice");
        assertThat(content).contains("2,bob");
        // header should appear only once at top
        assertThat(content.indexOf("id,name")).isEqualTo(content.lastIndexOf("id,name"));
    }

    @Test
    void deletePipelineDirectory_removesAllFiles(@TempDir Path tempDir) throws IOException {
        when(appProperties.export()).thenReturn(exportConfig(tempDir));
        var svc = createService();
        svc.createPipelineDirectory("user", 8L);
        Path dir = tempDir.resolve("user").resolve("8");
        Files.writeString(dir.resolve("segments").resolve("segment_00001.csv"), "x\n");
        assertThat(Files.exists(dir)).isTrue();

        svc.deletePipelineDirectory("user", 8L);
        assertThat(Files.exists(dir)).isFalse();
    }
}
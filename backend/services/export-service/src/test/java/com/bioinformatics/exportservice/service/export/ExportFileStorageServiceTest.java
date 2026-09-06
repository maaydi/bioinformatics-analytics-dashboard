package com.bioinformatics.exportservice.service.export;

import com.bioinformatics.exportservice.assembler.DelimitedSegmentAssembler;
import com.bioinformatics.exportservice.assembler.ExcelSegmentAssembler;
import com.bioinformatics.exportservice.assembler.JsonSegmentAssembler;
import com.bioinformatics.exportservice.assembler.SegmentAssemblerRegistry;
import com.bioinformatics.exportservice.config.ApplicationProperties;
import com.bioinformatics.exportservice.dto.ExportFormat;
import com.bioinformatics.exportservice.service.DefaultExportFileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExportFileStorageServiceTest {

    @TempDir
    Path tempDir;

    DefaultExportFileStorageService createService() {
        var properties =
                new ApplicationProperties(new ApplicationProperties.Export(new ApplicationProperties.Csv(10000), tempDir.toString()));

        var registry = new SegmentAssemblerRegistry(List.of(
                new DelimitedSegmentAssembler(),
                new JsonSegmentAssembler(),
                new ExcelSegmentAssembler()
        ));

        return new DefaultExportFileStorageService(properties, registry);
    }

    @AfterEach
    void cleanup() throws IOException {
        // TempDir is cleaned automatically by JUnit
    }

    @Test
    void createPipelineDirectory_createsExpectedStructure() throws IOException {
        var svc = createService();
        Path dir = svc.createPipelineDirectory(42L, 123L);
        assertThat(Files.exists(dir)).isTrue();
        assertThat(Files.exists(dir.resolve("segments"))).isTrue();
    }

    @Test
    void assembleSegments_concatenatesCsvFiles() throws IOException {
        var svc = createService();
        svc.createPipelineDirectory(1L, 2L);
        Path segDir = tempDir.resolve("1").resolve("2").resolve("segments");
        Files.writeString(segDir.resolve("segment_00001.csv"), "id,name\n1,alice\n", StandardCharsets.UTF_8);
        Files.writeString(segDir.resolve("segment_00002.csv"), "id,name\n2,bob\n", StandardCharsets.UTF_8);

        Path finalFile = svc.assembleSegments(1L, 2L, ExportFormat.CSV);
        String content = Files.readString(finalFile, StandardCharsets.UTF_8);

        assertThat(content).contains("id,name");
        assertThat(content).contains("1,alice");
        assertThat(content).contains("2,bob");
        // header should appear only once at top
        assertThat(content.indexOf("id,name")).isEqualTo(content.lastIndexOf("id,name"));
    }

    @Test
    void deletePipelineDirectory_removesAllFiles() throws IOException {
        var svc = createService();
        svc.createPipelineDirectory(7L, 8L);
        Path dir = tempDir.resolve("7").resolve("8");
        Files.writeString(dir.resolve("segments").resolve("segment_00001.csv"), "x\n");
        assertThat(Files.exists(dir)).isTrue();

        svc.deletePipelineDirectory(7L, 8L);
        assertThat(Files.exists(dir)).isFalse();
    }
}
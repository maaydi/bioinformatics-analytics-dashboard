package com.bioinformatics.exportservice.service;

import com.bioinformatics.exportservice.assembler.SegmentAssemblerRegistry;
import com.bioinformatics.exportservice.config.ApplicationProperties;
import com.bioinformatics.exportservice.dto.ExportFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultExportFileStorageService implements ExportFileStorageService {

    private final ApplicationProperties properties;
    private final SegmentAssemblerRegistry assemblerRegistry;

    @Override
    public Path createPipelineDirectory(Long userId, Long pipelineId) throws IOException {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(pipelineId);
        Path dir = getBaseDir().resolve(String.valueOf(userId)).resolve(String.valueOf(pipelineId));
        Path segments = dir.resolve("segments");
        Files.createDirectories(segments);
        return dir;
    }

    @Override
    public Path getSegmentPath(Long userId, Long pipelineId, int chunkNumber, ExportFormat format) {
        String ext = extensionFor(format);
        return getBaseDir().resolve(String.valueOf(userId))
                .resolve(String.valueOf(pipelineId))
                .resolve("segments")
                .resolve(String.format("segment_%05d.%s", chunkNumber, ext));
    }

    @Override
    public Path getFinalFilePath(Long userId, Long pipelineId, ExportFormat format) {
        String ext = extensionFor(format);
        return getBaseDir().resolve(String.valueOf(userId))
                .resolve(String.valueOf(pipelineId))
                .resolve(String.format("export_%d.%s", pipelineId, ext));
    }

    @Override
    public Path assembleSegments(Long userId, Long pipelineId, ExportFormat format) throws IOException {
        Path dir = getBaseDir().resolve(String.valueOf(userId)).resolve(String.valueOf(pipelineId));
        Path segmentsDir = dir.resolve("segments");
        if (!Files.exists(segmentsDir) || !Files.isDirectory(segmentsDir)) {
            throw new IOException("Segments directory not found: " + segmentsDir);
        }

        List<Path> segments;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(segmentsDir)) {
            segments = new ArrayList<>();
            for (Path p : stream) {
                if (Files.isRegularFile(p)) segments.add(p);
            }
        }
        segments = segments.stream()
                .sorted(Comparator.comparing(Path::getFileName))
                .collect(Collectors.toList());

        Path finalFile = getFinalFilePath(userId, pipelineId, format);
        Files.createDirectories(finalFile.getParent());

        assemblerRegistry.get(format).assemble(segments, finalFile);

        return finalFile;
    }


    @Override
    public void deletePipelineDirectory(Long userId, Long pipelineId) throws IOException {
        var dir = getBaseDir().resolve(String.valueOf(userId)).resolve(String.valueOf(pipelineId));
        if (!Files.exists(dir)) {
            log.warn("Pipeline folder {} does not exist", dir);
            return;
        }
        var deleted = FileSystemUtils.deleteRecursively(dir);
        log.info("Pipeline folder {} {}", dir, deleted ? "has been successfully deleted" : "could not be fully deleted");
    }

    @Override
    public long getFileSize(Long userId, Long pipelineId, ExportFormat format) throws IOException {
        Path finalFile = getFinalFilePath(userId, pipelineId, format);
        if (!Files.exists(finalFile)) return 0L;
        return Files.size(finalFile);
    }

    @Override
    public boolean validateFileExists(Long userId, Long pipelineId, ExportFormat format) {
        Path finalFile = getFinalFilePath(userId, pipelineId, format);
        return Files.exists(finalFile) && Files.isRegularFile(finalFile);
    }

    private Path getBaseDir() {
        return Paths.get(properties.export().tempDir()).toAbsolutePath().normalize();

    }


    private String extensionFor(ExportFormat format) {
        return switch (format) {
            case CSV -> "csv";
            case TSV -> "tsv";
            case JSON -> "json";
            case EXCEL -> "xlsx";
        };
    }
}


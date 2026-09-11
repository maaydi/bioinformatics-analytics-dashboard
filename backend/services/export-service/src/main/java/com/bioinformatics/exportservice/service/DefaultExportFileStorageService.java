package com.bioinformatics.exportservice.service;

import com.bioinformatics.exportservice.assembler.SegmentAssemblerRegistry;
import com.bioinformatics.exportservice.config.ApplicationProperties;
import com.bioinformatics.exportservice.dto.ExportFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;

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
        var dir = getBaseDir().resolve(String.valueOf(userId)).resolve(String.valueOf(pipelineId));
        var segments = dir.resolve("segments");
        Files.createDirectories(segments);
        return dir;
    }

    @Override
    public Path getSegmentPath(Long userId, Long pipelineId, int chunkNumber, ExportFormat format) {
        return getBaseDir().resolve(String.valueOf(userId))
                .resolve(String.valueOf(pipelineId))
                .resolve("segments")
                .resolve(String.format("segment_%05d.%s", chunkNumber, format.getFileExtension()));
    }

    @Override
    public Path getFinalFilePath(Long userId, Long pipelineId, ExportFormat format) {
        return getBaseDir().resolve(String.valueOf(userId))
                .resolve(String.valueOf(pipelineId))
                .resolve(String.format("export_%d.%s", pipelineId, format.getFileExtension()));
    }

    @Override
    public Path assembleSegments(Long userId, Long pipelineId, ExportFormat format) throws IOException {
        var dir = getBaseDir().resolve(String.valueOf(userId)).resolve(String.valueOf(pipelineId));
        var segmentsDir = dir.resolve("segments");
        if (!Files.exists(segmentsDir) || !Files.isDirectory(segmentsDir)) {
            throw new IOException("Segments directory not found: " + segmentsDir);
        }

        var segments = new ArrayList<Path>();
        try (var stream = Files.newDirectoryStream(segmentsDir)) {
            for (var p : stream) {
                if (Files.isRegularFile(p)) segments.add(p);
            }
        }

        var finalFile = getFinalFilePath(userId, pipelineId, format);
        Files.createDirectories(finalFile.getParent());
        var sortedSeg = segments.stream()
                .sorted(Comparator.comparing(Path::getFileName))
                .toList();
        assemblerRegistry.get(format).assemble(sortedSeg, finalFile);

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
        var finalFile = getFinalFilePath(userId, pipelineId, format);
        if (!Files.exists(finalFile)) return 0L;
        return Files.size(finalFile);
    }

    @Override
    public boolean validateFileExists(Long userId, Long pipelineId, ExportFormat format) {
        var finalFile = getFinalFilePath(userId, pipelineId, format);
        return Files.exists(finalFile) && Files.isRegularFile(finalFile);
    }

    private Path getBaseDir() {
        return Paths.get(properties.export().tempDir()).toAbsolutePath().normalize();

    }
}


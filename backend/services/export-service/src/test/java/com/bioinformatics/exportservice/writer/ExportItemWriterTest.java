package com.bioinformatics.exportservice.writer;

import com.bioinformatics.exportservice.dto.DefaultExportFormat;
import com.bioinformatics.exportservice.dto.ExportFormat;
import com.bioinformatics.exportservice.service.ExportFileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExportItemWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void restart_continuesAfterCommittedSegmentsWithoutOverwritingThem() throws Exception {
        var storage = new TestStorage(tempDir);
        var executionContext = new ExecutionContext();
        var fields = List.of("id", "name");

        var initialWriter = new ExportItemWriter(
                new CsvExportWriter(), storage, "user", 7L, DefaultExportFormat.CSV, fields);
        initialWriter.open(executionContext);
        initialWriter.write(new Chunk<>(List.of(Map.of("id", 1, "name", "Alice"))));
        initialWriter.update(executionContext);

        var restartedWriter = new ExportItemWriter(
                new CsvExportWriter(), storage, "user", 7L, DefaultExportFormat.CSV, fields);
        restartedWriter.open(executionContext);
        restartedWriter.write(new Chunk<>(List.of(Map.of("id", 2, "name", "Bob"))));
        restartedWriter.update(executionContext);

        var first = storage.getSegmentPath("user", 7L, 1, DefaultExportFormat.CSV);
        var second = storage.getSegmentPath("user", 7L, 2, DefaultExportFormat.CSV);
        assertThat(Files.readString(first)).contains("Alice");
        assertThat(Files.readString(second)).contains("Bob");
        assertThat(executionContext.getInt("export.segment-index")).isEqualTo(2);
    }

    private static final class TestStorage implements ExportFileStorageService {
        private final Path baseDirectory;

        private TestStorage(Path baseDirectory) {
            this.baseDirectory = baseDirectory;
        }

        @Override
        public Path createPipelineDirectory(String userId, Long pipelineId) throws IOException {
            var directory = baseDirectory.resolve(userId).resolve(pipelineId.toString());
            Files.createDirectories(directory.resolve("segments"));
            return directory;
        }

        @Override
        public Path getSegmentPath(String userId, Long pipelineId, int chunkNumber, ExportFormat format) {
            return baseDirectory.resolve(userId).resolve(pipelineId.toString()).resolve("segments")
                    .resolve("segment_%05d.%s".formatted(chunkNumber, format.getFileExtension()));
        }

        @Override
        public Path getFinalFilePath(String userId, Long pipelineId, ExportFormat format) {
            return baseDirectory.resolve(userId).resolve(pipelineId.toString())
                    .resolve("export_%d.%s".formatted(pipelineId, format.getFileExtension()));
        }

        @Override
        public Path assembleSegments(String userId, Long pipelineId, ExportFormat format) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deletePipelineDirectory(String userId, Long pipelineId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getFileSize(String userId, Long pipelineId, ExportFormat format) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean validateFileExists(String userId, Long pipelineId, ExportFormat format) {
            throw new UnsupportedOperationException();
        }
    }
}


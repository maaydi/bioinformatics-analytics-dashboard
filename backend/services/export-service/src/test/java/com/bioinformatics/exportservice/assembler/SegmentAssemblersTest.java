package com.bioinformatics.exportservice.assembler;

import com.bioinformatics.exportservice.dto.DefaultExportFormat;
import com.bioinformatics.exportservice.dto.ExportFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class SegmentAssemblersTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void delimitedAssembler_preservesFirstHeaderAndSkipsEmptySegments() throws IOException {
        var firstSegment = write("segment_00001.csv", "id,name\n1,Alice\n");
        var emptySegment = write("segment_00002.csv", "");
        var secondSegment = write("segment_00003.csv", "id,name\n2,Bob\n");
        var finalFile = tempDir.resolve("export.csv");

        new DelimitedSegmentAssembler().assemble(List.of(firstSegment, emptySegment, secondSegment), finalFile);

        assertThat(Files.readString(finalFile)).isEqualTo("id,name\n1,Alice\n2,Bob\n");
    }

    @Test
    void jsonAssembler_mergesSegmentsIntoOneValidArray() throws Exception {
        var firstSegment = write("segment_00001.json", "[ {\"accession\": \"P12345\"} ]");
        var emptySegment = write("segment_00002.json", "[]");
        var secondSegment = write("segment_00003.json", "[{\"accession\":\"Q67890\"}]");
        var finalFile = tempDir.resolve("export.json");

        new JsonSegmentAssembler().assemble(List.of(firstSegment, emptySegment, secondSegment), finalFile);

        var entries = OBJECT_MAPPER.readTree(Files.readString(finalFile));
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).get("accession").asText()).isEqualTo("P12345");
        assertThat(entries.get(1).get("accession").asText()).isEqualTo("Q67890");
    }

    @Test
    void excelAssembler_copiesOnlySegmentAndRejectsEmptyInput() throws IOException {
        var segment = write("segment_00001.xlsx", "workbook-content");
        var finalFile = tempDir.resolve("export.xlsx");
        var assembler = new ExcelSegmentAssembler();

        assembler.assemble(List.of(segment), finalFile);

        assertThat(Files.readString(finalFile)).isEqualTo("workbook-content");
        assertThatThrownBy(() -> assembler.assemble(List.of(), tempDir.resolve("empty.xlsx")))
                .isInstanceOf(IOException.class)
                .hasMessage("No excel segments to assemble");
    }

    @Test
    void registry_resolvesRegisteredAssemblerAndRejectsInvalidConfigurations() {
        var csvAssembler = new DelimitedSegmentAssembler();
        var registry = new SegmentAssemblerRegistry(List.of(csvAssembler));

        assertThat(registry.get(DefaultExportFormat.CSV)).isSameAs(csvAssembler);
        assertThat(registry.get(DefaultExportFormat.TSV)).isSameAs(csvAssembler);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.get(DefaultExportFormat.JSON))
                .withMessage("Unsupported format: JSON");

        assertThatIllegalStateException()
                .isThrownBy(() -> new SegmentAssemblerRegistry(List.of(csvAssembler, new CsvOnlyAssembler())))
                .withMessageContaining("Duplicate SegmentAssembler for format CSV");
    }

    private Path write(String fileName, String content) throws IOException {
        var path = tempDir.resolve(fileName);
        return Files.writeString(path, content);
    }

    private static final class CsvOnlyAssembler implements SegmentAssembler {

        @Override
        public Set<ExportFormat> supportedFormats() {
            return Set.of(DefaultExportFormat.CSV);
        }

        @Override
        public void assemble(List<Path> segments, Path finalFile) {
        }
    }
}


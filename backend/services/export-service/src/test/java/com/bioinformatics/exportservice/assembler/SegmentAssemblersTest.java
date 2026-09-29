package com.bioinformatics.exportservice.assembler;

import com.bioinformatics.exportservice.dto.DefaultExportFormat;
import com.bioinformatics.exportservice.dto.ExportFormat;
import com.bioinformatics.exportservice.writer.ExcelExportWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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
    void delimitedAssembler_preservesFirstHeaderAndMultilineRecords() throws IOException {
        var firstSegment = write("segment_00001.csv", "\uFEFFid,name\r\n1,\"Alice\r\nSmith\"\r\n");
        var emptySegment = write("segment_00002.csv", "");
        var secondSegment = write("segment_00003.csv", "id,name\r\n2,Bob\r\n");
        var finalFile = tempDir.resolve("export.csv");

        new DelimitedSegmentAssembler().assemble(List.of(firstSegment, emptySegment, secondSegment), finalFile);

        try (var parser = CSVParser.parse(Files.newBufferedReader(finalFile), CSVFormat.DEFAULT)) {
            var records = parser.getRecords();
            assertThat(records).hasSize(3);
            assertThat(records.get(1).get(1)).isEqualTo("Alice\r\nSmith");
            assertThat(records.get(2).get(1)).isEqualTo("Bob");
        }
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
    void excelAssembler_mergesEverySegmentAndRejectsEmptyInput() throws IOException {
        var segment = tempDir.resolve("segment_00001.xlsx");
        var secondSegment = tempDir.resolve("segment_00002.xlsx");
        writeWorkbook(segment, 1, "Alice");
        writeWorkbook(secondSegment, 2, "Bob");
        var finalFile = tempDir.resolve("export.xlsx");
        var assembler = new ExcelSegmentAssembler();

        assembler.assemble(List.of(segment, secondSegment), finalFile);

        try (var workbook = WorkbookFactory.create(Files.newInputStream(finalFile))) {
            var sheet = workbook.getSheet("Export");
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(3);
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Alice");
            assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("Bob");
        }
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

    private void writeWorkbook(Path path, int id, String name) throws IOException {
        var writer = new ExcelExportWriter();
        try (var output = Files.newOutputStream(path)) {
            var fields = List.of("id", "name");
            writer.writeHeader(fields, output);
            writer.writeRow(java.util.Map.of("id", id, "name", name), fields, output);
            writer.close(output);
        }
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


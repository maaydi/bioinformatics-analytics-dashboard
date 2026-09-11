package com.bioinformatics.exportservice.writer;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExportWritersTest {

    private CsvExportWriter csvExportWriter;
    private JsonExportWriter jsonExportWriter;
    private ExcelExportWriter excelExportWriter;

    private List<String> fields;
    private Map<String, Object> row1;
    private Map<String, Object> row2;

    @BeforeEach
    void setUp() {
        csvExportWriter = new CsvExportWriter();
        jsonExportWriter = new JsonExportWriter();
        excelExportWriter = new ExcelExportWriter();

        fields = List.of("id", "name", "isActive", "score");
        row1 = Map.of("id", 1, "name", "Alice", "isActive", true, "score", 95.5);
        row2 = Map.of("id", 2, "name", "Bob", "isActive", false, "score", 88.0);
    }

    @Test
    @DisplayName("CsvExportWriter: prepends UTF-8 BOM, formats RFC 4180 headers and rows")
    void testCsvExportWriter() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        csvExportWriter.writeHeader(fields, out);
        csvExportWriter.writeRow(row1, fields, out);
        csvExportWriter.writeRow(row2, fields, out);
        csvExportWriter.close(out);

        byte[] bytes = out.toByteArray();

        // Verify UTF-8 BOM bytes (0xEF, 0xBB, 0xBF)
        assertThat(bytes).hasSizeGreaterThanOrEqualTo(3);
        assertThat(bytes[0]).isEqualTo((byte) 0xEF);
        assertThat(bytes[1]).isEqualTo((byte) 0xBB);
        assertThat(bytes[2]).isEqualTo((byte) 0xBF);

        // Strip BOM and verify textual content
        String csvContent = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        String expected = """
                id,name,isActive,score\r
                1,Alice,true,95.5\r
                2,Bob,false,88.0\r
                """;

        assertThat(csvContent).isEqualTo(expected);
    }

    @Test
    @DisplayName("JsonExportWriter: streams valid JSON array of ordered objects")
    void testJsonExportWriter() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        jsonExportWriter.writeHeader(fields, out);
        jsonExportWriter.writeRow(row1, fields, out);
        jsonExportWriter.writeRow(row2, fields, out);
        jsonExportWriter.close(out);

        String jsonContent = out.toString(StandardCharsets.UTF_8);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonContent);

        assertThat(rootNode.isArray()).isTrue();
        assertThat(rootNode).hasSize(2);

        // First item verification
        JsonNode firstItem = rootNode.get(0);
        assertThat(firstItem.get("id").asInt()).isEqualTo(1);
        assertThat(firstItem.get("name").asText()).isEqualTo("Alice");
        assertThat(firstItem.get("isActive").asBoolean()).isTrue();
        assertThat(firstItem.get("score").asDouble()).isEqualTo(95.5);

        // Second item verification
        JsonNode secondItem = rootNode.get(1);
        assertThat(secondItem.get("id").asInt()).isEqualTo(2);
        assertThat(secondItem.get("name").asText()).isEqualTo("Bob");
    }

    @Test
    @DisplayName("ExcelExportWriter: generates valid XLSX workbook with typed cells and headers")
    void testExcelExportWriter() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        excelExportWriter.writeHeader(fields, out);
        excelExportWriter.writeRow(row1, fields, out);
        excelExportWriter.writeRow(row2, fields, out);
        excelExportWriter.close(out);

        byte[] bytes = out.toByteArray();
        assertThat(bytes).isNotEmpty();

        // Parse exported output using Apache POI
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("Export");
            assertThat(sheet).isNotNull();

            // Verify Header
            Row headerRow = sheet.getRow(0);
            assertThat(headerRow).isNotNull();
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("id");
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("name");
            assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("isActive");
            assertThat(headerRow.getCell(3).getStringCellValue()).isEqualTo("score");

            // Verify Data Row 1 (typed cell inspection)
            Row dataRow1 = sheet.getRow(1);
            assertThat(dataRow1.getCell(0).getNumericCellValue()).isEqualTo(1.0);
            assertThat(dataRow1.getCell(1).getStringCellValue()).isEqualTo("Alice");
            assertThat(dataRow1.getCell(2).getBooleanCellValue()).isTrue();
            assertThat(dataRow1.getCell(3).getNumericCellValue()).isEqualTo(95.5);

            // Verify Data Row 2
            Row dataRow2 = sheet.getRow(2);
            assertThat(dataRow2.getCell(0).getNumericCellValue()).isEqualTo(2.0);
            assertThat(dataRow2.getCell(1).getStringCellValue()).isEqualTo("Bob");
            assertThat(dataRow2.getCell(2).getBooleanCellValue()).isFalse();
            assertThat(dataRow2.getCell(3).getNumericCellValue()).isEqualTo(88.0);
        }
    }
}

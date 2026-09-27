package com.bioinformatics.exportservice.writer;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelExportWriterTest {

    @Test
    void finalization_flushesAndClosesWorkbookResources() throws IOException {
        var writer = new ExcelExportWriter();
        var output = new ByteArrayOutputStream();
        var fields = List.of("id", "name", "score");

        writer.writeHeader(fields, output);
        writer.writeRow(Map.of("id", 1, "name", "Alice", "score", 99.5), fields, output);
        writer.close(output);

        byte[] bytes = output.toByteArray();
        assertThat(bytes).isNotEmpty();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("Export");
            assertThat(sheet).isNotNull();
            Row header = sheet.getRow(0);
            assertThat(header).isNotNull();
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("id");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("name");
            assertThat(header.getCell(2).getStringCellValue()).isEqualTo("score");

            Row dataRow = sheet.getRow(1);
            assertThat(dataRow).isNotNull();
            assertThat(dataRow.getCell(0).getNumericCellValue()).isEqualTo(1.0);
            assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("Alice");
            assertThat(dataRow.getCell(2).getNumericCellValue()).isEqualTo(99.5);
        }
    }
}


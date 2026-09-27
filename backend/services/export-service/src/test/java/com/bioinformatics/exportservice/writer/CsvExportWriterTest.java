package com.bioinformatics.exportservice.writer;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExportWriterTest {

    @Test
    void writeRow_escapesCommasAndQuotes() throws IOException {
        var writer = new CsvExportWriter();
        var output = new ByteArrayOutputStream();

        writer.writeHeader(List.of("id", "notes"), output);
        writer.writeRow(Map.of("id", 1, "notes", "Text with, comma and \"quotes\" and \nnew line"),
                List.of("id", "notes"), output);
        writer.close(output);

        String csvContent = new String(output.toByteArray(), 3, output.size() - 3, StandardCharsets.UTF_8);
        assertThat(csvContent).contains("id,notes");
        assertThat(csvContent).contains("\"Text with, comma and \"\"quotes\"\" and \nnew line\"");
    }

    @Test
    void writeHeader_preservesCallerSelectedFieldOrder() throws IOException {
        var writer = new CsvExportWriter();
        var output = new ByteArrayOutputStream();
        var orderedFields = List.of("geneNamePrimary", "accession", "proteinFullName");
        var row = Map.<String, Object>of(
                "geneNamePrimary", "BRCA1",
                "accession", "P38398",
                "proteinFullName", "Breast cancer type 1 susceptibility protein"
        );

        writer.writeHeader(orderedFields, output);
        writer.writeRow(row, orderedFields, output);
        writer.close(output);

        String csvContent = new String(output.toByteArray(), 3, output.size() - 3, StandardCharsets.UTF_8);
        String[] lines = csvContent.split("\\r\\n", -1);

        assertThat(lines[0]).isEqualTo("geneNamePrimary,accession,proteinFullName");
        assertThat(lines[1]).startsWith("BRCA1,P38398,");
    }
}

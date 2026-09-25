package com.bioinformatics.exportservice.writer;

import com.bioinformatics.exportservice.dto.DefaultExportFormat;
import com.bioinformatics.exportservice.dto.ExportFormat;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ExportWriterFactoryTest {

    @Test
    void getWriter_returnsImplementationForRegisteredFormat() {
        var csvWriter = new StubWriter(DefaultExportFormat.CSV);
        var jsonWriter = new StubWriter(DefaultExportFormat.JSON);
        var factory = new ExportWriterFactory(List.of(csvWriter, jsonWriter));

        assertThat(factory.getWriter(DefaultExportFormat.CSV)).isSameAs(csvWriter);
        assertThat(factory.getWriter(DefaultExportFormat.JSON)).isSameAs(jsonWriter);
    }

    @Test
    void getWriter_rejectsUnsupportedFormat() {
        var factory = new ExportWriterFactory(List.of(new StubWriter(DefaultExportFormat.CSV)));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> factory.getWriter(DefaultExportFormat.EXCEL))
                .withMessage("No writer implementation registered for format: EXCEL");
    }

    @Test
    void constructor_rejectsMultipleWritersForSameFormat() {
        assertThatIllegalStateException()
                .isThrownBy(() -> new ExportWriterFactory(List.of(
                        new StubWriter(DefaultExportFormat.CSV),
                        new StubWriter(DefaultExportFormat.CSV))))
                .withMessageContaining("Duplicate key CSV");
    }

    private record StubWriter(ExportFormat format) implements ExportFormatWriter {

        @Override
        public void writeHeader(List<String> fields, OutputStream out) {
        }

        @Override
        public void writeRow(Map<String, Object> row, List<String> fields, OutputStream out) {
        }

        @Override
        public void close(OutputStream out) {
        }

        @Override
        public ExportFormat getFormat() {
            return format;
        }
    }
}


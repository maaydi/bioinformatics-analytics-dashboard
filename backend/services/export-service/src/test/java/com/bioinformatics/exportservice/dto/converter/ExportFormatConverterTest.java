package com.bioinformatics.exportservice.dto.converter;

import com.bioinformatics.exportservice.dto.DefaultExportFormat;
import com.bioinformatics.exportservice.dto.ExportFormat;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExportFormatConverterTest {

    private final ExportFormatConverter converter = new ExportFormatConverter();

    @Test
    void convertToDatabaseColumn_returnsEnumNameOrNull() {
        assertThat(converter.convertToDatabaseColumn(DefaultExportFormat.EXCEL)).isEqualTo("EXCEL");
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void convertToEntityAttribute_resolvesFormatsWithoutCaseSensitivity() {
        assertThat(converter.convertToEntityAttribute("csv")).isEqualTo(DefaultExportFormat.CSV);
        assertThat(converter.convertToEntityAttribute("TsV")).isEqualTo(DefaultExportFormat.TSV);
        assertThat(converter.convertToEntityAttribute("JSON")).isEqualTo(DefaultExportFormat.JSON);
        assertThat(converter.convertToEntityAttribute("excel")).isEqualTo(DefaultExportFormat.EXCEL);
    }

    @Test
    void convertToEntityAttribute_returnsNullForNullAndDefaultForUnknownValue() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("parquet")).isEqualTo(DefaultExportFormat.CSV);
    }

    @Test
    void register_makesAdditionalFormatAvailableToPersistenceConverter() {
        ExportFormatConverter.register(TestFormat.XML);

        assertThat(converter.convertToEntityAttribute("xml")).isEqualTo(TestFormat.XML);
        assertThat(converter.convertToDatabaseColumn(TestFormat.XML)).isEqualTo("XML");
    }

    private enum TestFormat implements ExportFormat {
        XML;

        @Override
        public String getFileExtension() {
            return "xml";
        }

        @Override
        public String getContentType() {
            return "application/xml";
        }
    }
}


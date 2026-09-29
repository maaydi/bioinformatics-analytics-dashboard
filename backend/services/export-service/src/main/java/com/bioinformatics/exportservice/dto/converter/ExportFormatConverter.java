package com.bioinformatics.exportservice.dto.converter;


import com.bioinformatics.exportservice.dto.DefaultExportFormat;
import com.bioinformatics.exportservice.dto.ExportFormat;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Converter
@Slf4j
public class ExportFormatConverter implements AttributeConverter<ExportFormat, String> {
    @Override
    public String convertToDatabaseColumn(ExportFormat attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public ExportFormat convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        var format = ExportFormatRegistry.getFormat(dbData);

        return Objects.requireNonNullElseGet(format, () -> {
            log.error("No such export format in DB: {}. Falling back to CSV.", dbData);
            return DefaultExportFormat.CSV;
        });
    }
}

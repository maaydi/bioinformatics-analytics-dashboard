package com.bioinformatics.exportservice.dto.converter;


import com.bioinformatics.exportservice.dto.DefaultExportFormat;
import com.bioinformatics.exportservice.dto.ExportFormat;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Converter
@Slf4j
public class ExportFormatConverter implements AttributeConverter<ExportFormat, String> {

    private static final Map<String, ExportFormat> REGISTRY = new ConcurrentHashMap<>();

    static {
        for (var format : ExportFormatRegistry.getAllAvailableFormats("com.bioinformatics")) {
            register(format);
        }
    }

    public static void register(ExportFormat format) {
        REGISTRY.put(format.name().toUpperCase(), format);
    }

    @Override
    public String convertToDatabaseColumn(ExportFormat attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public ExportFormat convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        var format = REGISTRY.get(dbData.toUpperCase());
        return Objects.requireNonNullElseGet(format, () -> {
            log.error("No such export format: {}", dbData);
            log.info("Returning default export format: {}", DefaultExportFormat.CSV);
            return DefaultExportFormat.CSV;
        });
    }
}

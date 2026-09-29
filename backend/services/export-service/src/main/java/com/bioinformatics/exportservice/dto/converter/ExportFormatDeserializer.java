package com.bioinformatics.exportservice.dto.converter;


import com.bioinformatics.exportservice.dto.ExportFormat;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class ExportFormatDeserializer extends ValueDeserializer<ExportFormat> {

    @Override
    public ExportFormat deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        var value = p.getValueAsString();
        if (value == null || value.isBlank()) {
            return null;
        }
        var format = ExportFormatRegistry.getFormat(value);
        if (format == null) {
            throw ctxt.weirdStringException(value, ExportFormat.class, "Unknown ExportFormat " + value);
        }
        return format;
    }
}
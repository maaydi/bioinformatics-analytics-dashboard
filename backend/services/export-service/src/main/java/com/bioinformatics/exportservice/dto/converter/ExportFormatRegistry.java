package com.bioinformatics.exportservice.dto.converter;

import com.bioinformatics.exportservice.dto.ExportFormat;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ExportFormatRegistry {

    private static final Map<String, ExportFormat> REGISTRY = new ConcurrentHashMap<>();

    public static void register(ExportFormat format) {
        if (format != null) {
            REGISTRY.put(format.name().toUpperCase(), format);
            log.info("Registered ExportFormat: {} [{}]", format.name(), format.getClass().getName());
        }
    }

    public static ExportFormat getFormat(String name) {
        if (name == null || name.isBlank()) return null;
        return REGISTRY.get(name.trim().toUpperCase());
    }

    public static Collection<ExportFormat> getAllAvailableFormats() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static void clear() {
        REGISTRY.clear();
    }
}

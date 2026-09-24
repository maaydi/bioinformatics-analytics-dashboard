package com.bioinformatics.exportservice.processor;

import com.bioinformatics.common.gene.dto.ProteinDetailDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Flattens ProteinDetailDto into a Map&lt;String, Object&gt; for export.
 * <p>
 * Flattening rules:
 * - Simple types (non-id): fieldName → value
 * - Composite types: fieldName → composite.toString()
 * - Collections of simple types: fieldName → "value1;value2;value3"
 * - Collections of composite types: fieldName → "toString1;toString2;toString3"
 * <p>
 * Database IDs (fields ending in 'Id' or named 'id' with numeric type) are skipped.
 * <p>
 * Example output keys:
 * - "accession" → "P12345"
 * - "proteinFullName" → "Full Name"
 * - "features" → "ProteinFeatureDto(featureType=transmembrane, ...);ProteinFeatureDto(featureType=signal peptide, ...)"
 * - "keywords" → "keyword1;keyword2;keyword3"
 * - "goTerms" → "GoTermDto(...);GoTermDto(...)"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProteinDetailProcessor implements ItemProcessor<ProteinDetailDto, Map<String, Object>> {

    @Override
    public Map<String, Object> process(@NonNull ProteinDetailDto item) {
        var flattenedMap = new LinkedHashMap<String, Object>();
        flattenRecord(item, flattenedMap);
        return flattenedMap;
    }

    /**
     * Flattens a record by iterating over its components.
     * Skips database ID fields and converts values according to their type.
     */
    private void flattenRecord(Object record, Map<String, Object> flattenedMap) {
        var components = record.getClass().getRecordComponents();

        for (var component : components) {
            try {
                var fieldName = component.getName();

                // Skip database ID fields
                if (isDatabaseIdField(fieldName, component.getType())) {
                    continue;
                }

                var value = component.getAccessor().invoke(record);
                if (value == null) {
                    continue;
                }

                var flattenedValue = flattenValue(value);
                flattenedMap.put(fieldName, flattenedValue);
            } catch (Exception e) {
                log.warn("Exception occurred while flattening record", e);
            }
        }
    }

    /**
     * Converts a value to its flattened representation.
     * - Simple types: return as-is
     * - Collections: join items with ";" using toString()
     * - Composite types: return toString()
     */
    private Object flattenValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Collection<?> collection) {
            return flattenCollection(collection);
        }

        if (isSimpleType(value)) {
            return value;
        }

        // For composite types (records or other objects), use toString()
        return value.toString();
    }

    /**
     * Flattens a collection by joining items with semicolon.
     */
    private String flattenCollection(Collection<?> collection) {
        if (collection.isEmpty()) {
            return "";
        }

        return collection.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .reduce((a, b) -> a + ";" + b)
                .orElse("");
    }

    /**
     * Checks if a field is a database ID field that should be skipped.
     * ID fields are: "id" that have numeric type.
     */
    private boolean isDatabaseIdField(String fieldName, Class<?> fieldType) {
        boolean isNumericType = isNumericType(fieldType);
        return isNumericType && (fieldName.equals("id"));
    }

    /**
     * Checks if a type is numeric (int, long, Integer, Long, etc.).
     */
    private boolean isNumericType(Class<?> clazz) {
        return clazz == int.class
                || clazz == long.class
                || clazz == short.class
                || clazz == byte.class
                || clazz == Integer.class
                || clazz == Long.class
                || clazz == Short.class
                || clazz == Byte.class;
    }

    /**
     * Checks if a value is a simple/primitive type.
     */
    private boolean isSimpleType(Object value) {
        if (value == null) {
            return true;
        }

        Class<?> clazz = value.getClass();
        return clazz.isPrimitive()
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof java.time.LocalDate
                || value instanceof java.time.LocalDateTime
                || value instanceof java.time.Instant
                || value instanceof java.util.Date
                || value instanceof Enum;
    }
}

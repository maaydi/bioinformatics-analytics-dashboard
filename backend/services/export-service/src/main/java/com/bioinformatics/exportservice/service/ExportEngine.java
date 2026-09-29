package com.bioinformatics.exportservice.service;

import com.bioinformatics.shared.models.gene.ExportFieldSchema;
import com.bioinformatics.shared.models.gene.export.ExportField;
import com.bioinformatics.shared.models.gene.export.ExportIgnore;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

/**
 * Utility for introspecting and extracting export field metadata.
 *
 * <p>Uses Java reflection on record types to:
 * <ul>
 *   <li>Discover exportable fields (marked with @ExportField)
 *   <li>Generate field schema (name, display name, data type, description)
 *   <li>Identify excluded fields (marked with @ExportIgnore)
 * </ul>
 *
 * <p>Example:
 * <pre>
 * var fields = ExportEngine.getAvailableFieldsForExport(ProteinDetailDto.class);
 * var excluded = ExportEngine.getExcludedFields(ProteinDetailDto.class);
 * </pre>
 *
 * @see com.bioinformatics.shared.models.gene.export.ExportField
 * @see com.bioinformatics.shared.models.gene.export.ExportIgnore
 */
public class ExportEngine {

    /**
     * Extracts fields marked for exclusion from export.
     *
     * @param clazz target class (must be a record)
     * @param <T>   record type
     * @return list of field names to exclude
     */
    public static <T> List<String> getExcludedFields(Class<T> clazz) {
        if (!clazz.isRecord()) return List.of();

        return Arrays.stream(clazz.getRecordComponents())
                .filter(c -> c.isAnnotationPresent(ExportIgnore.class))
                .map(RecordComponent::getName)
                .toList();
    }

    /**
     * Extracts exportable fields and their schemas.
     *
     * <p>Scans record components for @ExportField annotation
     * and builds field schema for UI form rendering.
     *
     * @param clazz target class (must be a record)
     * @param <T>   record type
     * @return list of exportable field schemas (name, display name, type, description)
     */
    public static <T> List<ExportFieldSchema> getAvailableFieldsForExport(Class<T> clazz) {
        if (!clazz.isRecord()) {
            return List.of();
        }

        return Arrays.stream(clazz.getRecordComponents())
                .filter(component -> component.isAnnotationPresent(ExportField.class))
                .map(ExportEngine::mapToSchema)
                .toList();
    }

    /**
     * Maps a record component to export field schema.
     *
     * <p>Resolves:
     * <ul>
     *   <li>Field name (from annotation or component name)
     *   <li>Display name (human-readable label)
     *   <li>Data type (from annotation or component type)
     *   <li>Description (help text for UI)
     *   <li>Availability (whether user can select this field)
     * </ul>
     *
     * @param component record component with @ExportField annotation
     * @return field schema
     */
    private static ExportFieldSchema mapToSchema(RecordComponent component) {
        var annotation = component.getAnnotation(ExportField.class);

        var fieldName = annotation.name().isBlank()
                ? component.getName()
                : annotation.name();

        var displayName = annotation.displayName().isBlank()
                ? fieldName
                : annotation.displayName();

        var dataType = annotation.dataType().equals(String.class)
                ? component.getType().getSimpleName()
                : annotation.dataType().getSimpleName();

        return new ExportFieldSchema(
                fieldName,
                displayName,
                dataType,
                annotation.description(),
                annotation.available()
        );
    }
}

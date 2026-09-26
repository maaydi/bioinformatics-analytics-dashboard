package com.bioinformatics.exportservice.service;

import com.bioinformatics.shared.models.gene.ExportFieldSchema;
import com.bioinformatics.shared.models.gene.export.ExportField;
import com.bioinformatics.shared.models.gene.export.ExportIgnore;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

public class ExportEngine {

    public static <T> List<String> getExcludedFields(Class<T> clazz) {
        if (!clazz.isRecord()) return List.of();

        return Arrays.stream(clazz.getRecordComponents())
                .filter(c -> c.isAnnotationPresent(ExportIgnore.class))
                .map(RecordComponent::getName)
                .toList();
    }

    public static <T> List<ExportFieldSchema> getAvailableFieldsForExport(Class<T> clazz) {
        if (!clazz.isRecord()) {
            return List.of();
        }

        return Arrays.stream(clazz.getRecordComponents())
                .filter(component -> component.isAnnotationPresent(ExportField.class))
                .map(ExportEngine::mapToSchema)
                .toList();
    }

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

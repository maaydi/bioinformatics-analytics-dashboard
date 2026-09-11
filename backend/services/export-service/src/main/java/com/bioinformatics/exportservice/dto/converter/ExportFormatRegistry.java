package com.bioinformatics.exportservice.dto.converter;

import com.bioinformatics.exportservice.dto.ExportFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ExportFormatRegistry {

    public static List<ExportFormat> getAllAvailableFormats(String basePackage) {
        var allFormats = new ArrayList<ExportFormat>();
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(ExportFormat.class));
        for (var bd : scanner.findCandidateComponents(basePackage)) {
            try {
                var clazz = Class.forName(bd.getBeanClassName());
                if (clazz.isEnum()) {
                    log.info("Found enum {} implements {}", clazz.getName(), ExportFormat.class.getName());
                    var enumConstants = (ExportFormat[]) clazz.getEnumConstants();
                    if (enumConstants != null) {
                        allFormats.addAll(List.of(enumConstants));
                    }
                }
            } catch (ClassNotFoundException e) {
                log.error("Failed to get all available format {}", e.getMessage(), e);
            }
        }

        return allFormats;
    }
}


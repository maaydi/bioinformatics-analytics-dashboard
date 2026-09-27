package com.bioinformatics.exportservice.service;

import com.bioinformatics.exportservice.config.ApplicationProperties;
import com.bioinformatics.exportservice.dto.ExportFormat;
import com.bioinformatics.exportservice.dto.converter.ExportFormatRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExportFormatInitializer {

    private final ResourceLoader resourceLoader;
    private final Environment environment;
    private final List<ExportFormat> springBeanFormats;
    private final ApplicationProperties applicationProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeExportFormats() {
        log.info("Initializing ExportFormatRegistry scanning package: {}", applicationProperties.export().basePackage());

        ExportFormatRegistry.clear();

        for (var format : springBeanFormats) {
            ExportFormatRegistry.register(format);
        }

        var scanner = new ClassPathScanningCandidateComponentProvider(false, environment) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isIndependent();
            }
        };

        scanner.setResourceLoader(resourceLoader);
        scanner.addIncludeFilter(new AssignableTypeFilter(ExportFormat.class));

        for (var bd : scanner.findCandidateComponents(applicationProperties.export().basePackage())) {
            try {
                Class<?> clazz = ClassUtils.forName(Objects.requireNonNull(bd.getBeanClassName()), resourceLoader.getClassLoader());

                if (ExportFormat.class.isAssignableFrom(clazz) && clazz.isEnum()) {
                    ExportFormat[] enumConstants = (ExportFormat[]) clazz.getEnumConstants();
                    if (enumConstants != null) {
                        for (ExportFormat format : enumConstants) {
                            ExportFormatRegistry.register(format);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to register ExportFormat class: {}", bd.getBeanClassName(), e);
            }
        }

        log.info("ExportFormatRegistry initialized with {} total formats.",
                ExportFormatRegistry.getAllAvailableFormats().size());
    }
}

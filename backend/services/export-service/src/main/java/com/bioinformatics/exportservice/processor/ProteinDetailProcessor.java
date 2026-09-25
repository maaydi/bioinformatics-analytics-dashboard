package com.bioinformatics.exportservice.processor;

import com.bioinformatics.common.gene.dto.ProteinDetailDto;
import com.bioinformatics.exportservice.batch.ExportJobParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Flattens ProteinDetailDto into a Map&lt;String, Object&gt; for export.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@StepScope
public class ProteinDetailProcessor implements ItemProcessor<ProteinDetailDto, Map<String, Object>> {

    private final ExportJobParameters parameters;

    @Override
    public Map<String, Object> process(@NonNull ProteinDetailDto item) {
        var filteredMap = new LinkedHashMap<>(item.row());
        filteredMap.keySet().retainAll(parameters.getExportedFields());
        return filteredMap;
    }
}

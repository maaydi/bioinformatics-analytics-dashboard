package com.bioinformatics.exportservice.processor;

import com.bioinformatics.common.gene.dto.ProteinDetailDto;
import com.bioinformatics.exportservice.batch.ExportJobParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
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
        var fields = new HashSet<>(parameters.getExportedFields()); // to Asset O(1) instead of O(M) for list
        filteredMap.keySet().retainAll(fields);
        return filteredMap;
    }
}

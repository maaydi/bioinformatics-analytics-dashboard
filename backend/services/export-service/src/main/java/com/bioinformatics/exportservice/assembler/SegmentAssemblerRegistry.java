package com.bioinformatics.exportservice.assembler;


import com.bioinformatics.exportservice.dto.ExportFormat;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class SegmentAssemblerRegistry {

    private final Map<ExportFormat, SegmentAssembler> byFormat = new EnumMap<>(ExportFormat.class);

    public SegmentAssemblerRegistry(List<SegmentAssembler> assemblers) {
        for (SegmentAssembler assembler : assemblers) {
            for (ExportFormat format : assembler.supportedFormats()) {
                SegmentAssembler existing = byFormat.putIfAbsent(format, assembler);
                if (existing != null) {
                    throw new IllegalStateException(
                            "Duplicate SegmentAssembler for format " + format +
                                    ": " + existing.getClass().getSimpleName() +
                                    " and " + assembler.getClass().getSimpleName());
                }
            }
        }
    }

    public SegmentAssembler get(ExportFormat format) {
        SegmentAssembler assembler = byFormat.get(format);
        if (assembler == null) {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }
        return assembler;
    }
}

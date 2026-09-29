package com.bioinformatics.exportservice.writer;


import com.bioinformatics.exportservice.dto.ExportFormat;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ExportWriterFactory {

    private final Map<ExportFormat, ExportFormatWriter> writerMap;

    public ExportWriterFactory(List<ExportFormatWriter> writers) {
        this.writerMap = writers.stream()
                .collect(Collectors.toMap(ExportFormatWriter::getFormat, Function.identity()));
    }

    public ExportFormatWriter getWriter(ExportFormat format) {
        var writer = writerMap.get(format);
        if (writer == null) {
            throw new IllegalArgumentException("No writer implementation registered for format: " + format);
        }
        return writer;
    }
}

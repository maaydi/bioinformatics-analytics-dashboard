package com.bioinformatics.exportservice.writer;

import com.bioinformatics.exportservice.dto.DefaultExportFormat;
import com.bioinformatics.exportservice.dto.ExportFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JsonExportWriter implements ExportFormatWriter {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<OutputStream, SequenceWriter> writers = new ConcurrentHashMap<>();

    @Override
    public void writeHeader(List<String> fields, OutputStream out) throws IOException {
        var writer = mapper.writerWithDefaultPrettyPrinter()
                .writeValuesAsArray(out);
        writers.put(out, writer);
    }

    @Override
    public void writeRow(Map<String, Object> row, List<String> fields, OutputStream out) throws IOException {
        var writer = writers.get(out);
        if (writer != null) {
            var orderRow = new LinkedHashMap<String, Object>();
            fields.forEach(field -> orderRow.put(field, row.get(field)));
            writer.write(orderRow);
        }
    }

    @Override
    public void close(OutputStream out) throws IOException {
        var writer = writers.remove(out);
        if (writer != null) {
            writer.close();
        }
    }


    @Override
    public ExportFormat getFormat() {
        return DefaultExportFormat.JSON;
    }
}

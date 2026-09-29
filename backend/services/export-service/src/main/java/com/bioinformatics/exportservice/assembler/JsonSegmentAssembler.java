package com.bioinformatics.exportservice.assembler;

import com.bioinformatics.exportservice.dto.DefaultExportFormat;
import com.bioinformatics.exportservice.dto.ExportFormat;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Component
public class JsonSegmentAssembler implements SegmentAssembler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Set<ExportFormat> supportedFormats() {
        return Set.of(DefaultExportFormat.JSON);
    }

    @Override
    public void assemble(List<Path> segments, Path finalFile) throws IOException {
        try (var generator = objectMapper.getFactory().createGenerator(Files.newOutputStream(finalFile))) {
            generator.writeStartArray();
            for (Path seg : segments) {
                copyArrayElements(seg, generator);
            }
            generator.writeEndArray();
        }
    }

    private void copyArrayElements(Path segment, com.fasterxml.jackson.core.JsonGenerator generator) throws IOException {
        try (var parser = objectMapper.getFactory().createParser(Files.newInputStream(segment))) {
            if (parser.nextToken() == null) {
                return;
            }
            if (!parser.hasToken(JsonToken.START_ARRAY)) {
                throw new IOException("JSON segment must contain one array: " + segment);
            }
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (parser.currentToken() == null) {
                    throw new IOException("JSON segment ended before its array was closed: " + segment);
                }
                generator.copyCurrentStructure(parser);
            }
        }
    }
}
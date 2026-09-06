package com.bioinformatics.exportservice.assembler;

import com.bioinformatics.exportservice.dto.ExportFormat;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
public class JsonSegmentAssembler implements SegmentAssembler {

    @Override
    public Set<ExportFormat> supportedFormats() {
        return EnumSet.of(ExportFormat.JSON);
    }

    @Override
    public void assemble(List<Path> segments, Path finalFile) throws IOException {
        try (var out = new BufferedOutputStream(Files.newOutputStream(finalFile))) {
            out.write('[');
            boolean firstWritten = false;
            for (Path seg : segments) {
                String content = Files.readString(seg, StandardCharsets.UTF_8).trim();
                if (content.isEmpty()) continue;
                if (content.startsWith("[")) content = content.substring(1);
                if (content.endsWith("]")) content = content.substring(0, content.length() - 1);
                content = content.trim();
                if (content.isEmpty()) continue;
                if (firstWritten) out.write(',');
                out.write(content.getBytes(StandardCharsets.UTF_8));
                firstWritten = true;
            }
            out.write(']');
        }
    }
}
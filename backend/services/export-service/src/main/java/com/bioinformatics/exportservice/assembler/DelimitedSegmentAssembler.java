package com.bioinformatics.exportservice.assembler;

import com.bioinformatics.exportservice.dto.ExportFormat;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
public class DelimitedSegmentAssembler implements SegmentAssembler {

    @Override
    public Set<ExportFormat> supportedFormats() {
        return EnumSet.of(ExportFormat.CSV, ExportFormat.TSV);
    }

    @Override
    public void assemble(List<Path> segments, Path finalFile) throws IOException {
        boolean first = true;
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(finalFile))) {
            for (Path seg : segments) {
                List<String> lines = Files.readAllLines(seg, StandardCharsets.UTF_8);
                if (lines.isEmpty()) continue;
                int start = first ? 0 : 1; // skip header on subsequent segments
                for (int i = start; i < lines.size(); i++) {
                    out.write(lines.get(i).getBytes(StandardCharsets.UTF_8));
                    out.write('\n');
                }
                first = false;
            }
        }
    }
}
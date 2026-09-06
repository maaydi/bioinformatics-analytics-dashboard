package com.bioinformatics.exportservice.assembler;

import com.bioinformatics.exportservice.dto.ExportFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class ExcelSegmentAssembler implements SegmentAssembler {

    @Override
    public Set<ExportFormat> supportedFormats() {
        return EnumSet.of(ExportFormat.EXCEL);
    }

    @Override
    public void assemble(List<Path> segments, Path finalFile) throws IOException {
        if (segments.isEmpty()) {
            throw new IOException("No excel segments to assemble");
        }
        if (segments.size() > 1) {
            log.warn("Merging multiple Excel segments into one XLSX is not fully supported. Using first segment as final file.");
        }
        Files.copy(segments.getFirst(), finalFile);
    }
}
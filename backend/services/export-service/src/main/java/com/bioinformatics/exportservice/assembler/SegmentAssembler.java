package com.bioinformatics.exportservice.assembler;


import com.bioinformatics.exportservice.dto.ExportFormat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public interface SegmentAssembler {
    Set<ExportFormat> supportedFormats();

    void assemble(List<Path> segments, Path finalFile) throws IOException;
}
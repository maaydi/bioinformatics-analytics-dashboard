package com.bioinformatics.exportservice.assembler;

import com.bioinformatics.exportservice.dto.DefaultExportFormat;
import com.bioinformatics.exportservice.dto.ExportFormat;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Component
public class DelimitedSegmentAssembler implements SegmentAssembler {

    @Override
    public Set<ExportFormat> supportedFormats() {
        return Set.of(DefaultExportFormat.CSV, DefaultExportFormat.TSV);
    }

    @Override
    public void assemble(List<Path> segments, Path finalFile) throws IOException {
        boolean first = true;
        boolean tabSeparated = finalFile.getFileName().toString().endsWith(DefaultExportFormat.TSV.getFileExtension());
        var format = tabSeparated
                ? CSVFormat.TDF.builder().setRecordSeparator("\r\n").get()
                : CSVFormat.DEFAULT.builder().setRecordSeparator("\r\n").get();
        try (var out = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(finalFile), StandardCharsets.UTF_8));
             var printer = new CSVPrinter(out, format)) {
            if (!tabSeparated) {
                out.write('\uFEFF');
            }
            for (var seg : segments) {
                try (var input = withoutUtf8Bom(Files.newInputStream(seg));
                     var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
                     var parser = CSVParser.parse(reader, format)) {
                    boolean header = true;
                    for (CSVRecord record : parser) {
                        if (header) {
                            header = false;
                            if (!first) {
                                continue;
                            }
                        }
                        printer.printRecord(record);
                    }
                }
                first = false;
            }
        }
    }

    private InputStream withoutUtf8Bom(InputStream input) throws IOException {
        var buffered = new BufferedInputStream(input);
        buffered.mark(3);
        if (buffered.read() != 0xEF || buffered.read() != 0xBB || buffered.read() != 0xBF) {
            buffered.reset();
        }
        return buffered;
    }
}
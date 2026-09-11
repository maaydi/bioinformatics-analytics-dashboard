package com.bioinformatics.exportservice.writer;

import com.bioinformatics.exportservice.dto.DefaultExportFormat;
import com.bioinformatics.exportservice.dto.ExportFormat;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CsvExportWriter implements ExportFormatWriter {

    private final Map<OutputStream, CSVPrinter> printers = new ConcurrentHashMap<>();

    @Override
    public void writeHeader(List<String> fields, OutputStream out) throws IOException {
        // Write UTF-8 BOM (\uFEFF)
        out.write(0xEF);
        out.write(0xBB);
        out.write(0xBF);
        var writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        var printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setRecordSeparator("\r\n").get());
        printer.printRecord(fields);
        printer.flush();
        printers.put(out, printer);
    }

    @Override
    public void writeRow(Map<String, Object> row, List<String> fields, OutputStream out) throws IOException {
        var printer = printers.get(out);
        if (printer != null) {
            var values = fields.stream().map(row::get).toList();
            printer.printRecord(values);
        }
    }

    @Override
    public void close(OutputStream out) throws IOException {
        var printer = printers.remove(out);
        if (printer != null) {
            printer.flush();
            printer.close();
        }

    }


    @Override
    public ExportFormat getFormat() {
        return DefaultExportFormat.CSV;
    }
}

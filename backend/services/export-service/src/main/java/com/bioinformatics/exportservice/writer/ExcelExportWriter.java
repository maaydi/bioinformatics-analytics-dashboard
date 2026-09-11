package com.bioinformatics.exportservice.writer;

import com.bioinformatics.exportservice.dto.DefaultExportFormat;
import com.bioinformatics.exportservice.dto.ExportFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

@Component
public class ExcelExportWriter implements ExportFormatWriter {

    private final Map<OutputStream, ExcelContext> contexts = new ConcurrentHashMap<>();

    @Override
    public void writeHeader(List<String> fields, OutputStream out) throws IOException {
        var workbook = new SXSSFWorkbook(100);
        var sheet = workbook.createSheet("Export");
        sheet.trackAllColumnsForAutoSizing();

        var headerRow = sheet.createRow(0);
        for (int i = 0; i < fields.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(fields.get(i));
        }

        sheet.createFreezePane(0, 1);

        var ctx = new ExcelContext(workbook, sheet);
        ctx.rowIndex = 1;
        contexts.put(out, ctx);
    }

    @Override
    public void writeRow(Map<String, Object> row, List<String> fields, OutputStream out) throws IOException {
        var ctx = contexts.get(out);
        if (ctx != null) {
            var dataRow = ctx.sheet.createRow(ctx.rowIndex++);
            IntStream.range(0, fields.size())
                    .forEach(i -> {
                        var cell = dataRow.createCell(i);
                        var val = row.get(fields.get(i));
                        switch (val) {
                            case null -> cell.setCellValue("");
                            case Number num -> cell.setCellValue(num.doubleValue());
                            case Boolean b -> cell.setCellValue(b);
                            default -> cell.setCellValue(val.toString());
                        }
                    });
        }
    }

    @Override
    public void close(OutputStream out) throws IOException {
        var ctx = contexts.remove(out);
        if (ctx != null) {
            var headerRow = ctx.sheet.getRow(0);
            if (headerRow != null) {
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    ctx.sheet.autoSizeColumn(i);
                }
            }
            ctx.workbook.write(out);
            ctx.workbook.close();
        }
    }

    @Override
    public ExportFormat getFormat() {
        return DefaultExportFormat.EXCEL;
    }

    private static class ExcelContext {
        final SXSSFWorkbook workbook;
        final SXSSFSheet sheet;
        int rowIndex = 0;

        ExcelContext(SXSSFWorkbook workbook, SXSSFSheet sheet) {
            this.workbook = workbook;
            this.sheet = sheet;
        }
    }
}

package com.bioinformatics.exportservice.assembler;

import com.bioinformatics.exportservice.dto.DefaultExportFormat;
import com.bioinformatics.exportservice.dto.ExportFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Component
public class ExcelSegmentAssembler implements SegmentAssembler {

    @Override
    public Set<ExportFormat> supportedFormats() {
        return Set.of(DefaultExportFormat.EXCEL);
    }

    @Override
    public void assemble(List<Path> segments, Path finalFile) throws IOException {
        if (segments.isEmpty()) {
            throw new IOException("No excel segments to assemble");
        }
        try (var result = new SXSSFWorkbook(100)) {
            var targetSheet = result.createSheet("Export");
            targetSheet.trackAllColumnsForAutoSizing();
            int targetRowIndex = 0;
            for (var segment : segments) {
                targetRowIndex = copySegment(segment, targetSheet, targetRowIndex, targetRowIndex > 0);
                targetSheet.flushRows(100);
            }
            targetSheet.createFreezePane(0, 1);
            autoSizeColumns(targetSheet);
            try (OutputStream output = Files.newOutputStream(finalFile)) {
                result.write(output);
            }
        }
    }

    private int copySegment(Path segment, org.apache.poi.xssf.streaming.SXSSFSheet targetSheet,
                            int targetRowIndex, boolean skipHeader) throws IOException {
        try (var input = Files.newInputStream(segment); var source = WorkbookFactory.create(input)) {
            var sourceSheet = source.getSheetAt(0);
            for (Row sourceRow : sourceSheet) {
                if (skipHeader && sourceRow.getRowNum() == sourceSheet.getFirstRowNum()) {
                    continue;
                }
                var targetRow = targetSheet.createRow(targetRowIndex++);
                for (Cell sourceCell : sourceRow) {
                    copyCell(sourceCell, targetRow.createCell(sourceCell.getColumnIndex()));
                }
            }
            return targetRowIndex;
        }
    }

    private void copyCell(Cell source, Cell target) {
        switch (source.getCellType()) {
            case BLANK -> target.setBlank();
            case BOOLEAN -> target.setCellValue(source.getBooleanCellValue());
            case ERROR -> target.setCellErrorValue(source.getErrorCellValue());
            case FORMULA -> target.setCellFormula(source.getCellFormula());
            case NUMERIC -> target.setCellValue(source.getNumericCellValue());
            case STRING -> target.setCellValue(source.getStringCellValue());
            case _NONE -> throw new IllegalStateException("Unsupported cell type: " + CellType._NONE);
        }
    }

    private void autoSizeColumns(org.apache.poi.xssf.streaming.SXSSFSheet sheet) {
        var header = sheet.getRow(0);
        if (header == null) {
            return;
        }
        for (int column = 0; column < header.getLastCellNum(); column++) {
            sheet.autoSizeColumn(column);
        }
    }
}
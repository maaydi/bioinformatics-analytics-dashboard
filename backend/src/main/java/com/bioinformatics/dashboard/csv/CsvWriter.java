package com.bioinformatics.dashboard.csv;

import com.bioinformatics.dashboard.config.AppProperties;
import com.bioinformatics.dashboard.exception.PayloadTooLargeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CsvWriter {

    private final AppProperties appProperties;


    public <T extends CsvSerializable> void write(Writer writer, List<T> items) throws IOException {
        if (items == null || items.isEmpty()) {
            return;
        }
        if (items.size() > appProperties.getExport().getCsv().getMaxRows()) {
            throw new PayloadTooLargeException("Export limit exceeded. Maximum allowed rows: "
                    + appProperties.getExport().getCsv().getMaxRows());
        }
        writer.write(items.getFirst().header());
        writer.write("\n");

        for (T item : items) {
            writer.write(item.row() + "\n");
        }
        writer.flush();
    }
}

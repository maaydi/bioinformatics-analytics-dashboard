package com.bioinformatics.dashboard.csv;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

@RequiredArgsConstructor
/**
 * Simple CSV writer that emits a header row followed by item rows.
 * Expects items implementing {@link CsvSerializable}.
 */
public class CsvWriter {
    public <T extends CsvSerializable> void write(Writer writer, List<T> items) throws IOException {
        if (items == null || items.isEmpty()) {
            return;
        }
        writer.write(items.getFirst().header());
        writer.write("\n");
        for (T item : items) {
            writer.write(item.row() + "\n");
        }
        writer.flush();
    }
}

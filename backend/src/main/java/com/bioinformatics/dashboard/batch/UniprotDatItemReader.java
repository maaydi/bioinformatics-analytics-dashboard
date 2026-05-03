package com.bioinformatics.dashboard.batch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.core.io.Resource;

public class UniprotDatItemReader implements ItemStreamReader<String> {

    private final Resource resource;
    private BufferedReader reader;

    public UniprotDatItemReader(Resource resource) {
        this.resource = resource;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        try {
            reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
        } catch (IOException e) {
            throw new ItemStreamException("Failed to initialize reader", e);
        }

    }

    @Override
    public @Nullable String read() throws Exception {
        if (reader == null) {
            return null;
        }
        var rec = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            rec.append(line).append("\n");
            if (line.trim().equals("//")) {
                return rec.toString();
            }
        }
        return rec.length() > 0 ? rec.toString() : null;
    }

    @Override
    public void close() throws ItemStreamException {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            throw new ItemStreamException("Error closing reader.", e);
        }
    }
}

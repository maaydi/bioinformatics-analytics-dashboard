package com.bioinformatics.dashboard.job.uniprot.fileloader.counter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Common base for record counters that operate on text streams.
 * Handles reader creation and delegates the actual counting to subclasses.
 */
public abstract class AbstractUniprotCounter implements RecordCounter {

    @Override
    public long count(InputStream content) throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(content, StandardCharsets.UTF_8))) {
            return doCount(reader);
        }
    }

    protected abstract long doCount(BufferedReader reader) throws IOException;
}

package com.bioinformatics.dashboard.batch.counter;

import java.io.IOException;
import java.io.InputStream;

/**
 * Strategy interface for counting records in an import file.
 * Implementations decide supported file types and provide a counting routine.
 */
public interface RecordCounter {
    boolean supports(String filename);

    long count(InputStream content) throws IOException;
}

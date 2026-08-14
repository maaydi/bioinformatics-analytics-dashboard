package com.bioinformatics.dashboard.job.uniprot.fileloader.counter;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Manages operations and logic for TsvUniprotCounter.
 */
@Component
public class TsvUniprotCounter extends AbstractUniprotCounter {
    private static final String EXTENSION = ".tsv";

    /**
     * Counts non-empty lines in a TSV file (suitable for simple row counts).
     */
    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(EXTENSION);
    }


    @Override
    protected long doCount(BufferedReader reader) throws IOException {
        return reader.lines()
                .filter(line -> !line.isBlank())
                .count();
    }

}

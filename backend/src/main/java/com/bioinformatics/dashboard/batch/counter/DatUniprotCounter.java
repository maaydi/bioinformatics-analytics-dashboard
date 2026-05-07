package com.bioinformatics.dashboard.batch.counter;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;

@Component
public class DatUniprotCounter extends AbstractUniprotCounter {
    private static final String DELIMITER = "//";
    private static final String EXTENSION = ".dat";

    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(EXTENSION);
    }


    @Override
    protected long doCount(BufferedReader reader) throws IOException {
        var count = 0L;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().equals(DELIMITER)) {
                count++;
            }
        }
        return count;
    }

}

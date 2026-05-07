package com.bioinformatics.dashboard.batch.counter;

import java.io.IOException;
import java.io.InputStream;

public interface RecordCounter {
    boolean supports(String filename);

    long count(InputStream content) throws IOException;
}

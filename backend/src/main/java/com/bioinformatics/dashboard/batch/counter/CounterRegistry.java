package com.bioinformatics.dashboard.batch.counter;

import com.bioinformatics.dashboard.exception.UnsupportedFileTypeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CounterRegistry {

    private final List<RecordCounter> counters;

    /**
     * Returns the first registered counter that supports the given filename.
     * Throws UnsupportedFileTypeException when none match.
     */
    public RecordCounter getCounter(String filename) {
        return counters.stream()
                .filter(c -> c.supports(filename))
                .findFirst()
                .orElseThrow(() -> new UnsupportedFileTypeException(
                        "No counter found for file: " + filename));
    }
}

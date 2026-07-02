package com.bioinformatics.dashboard.batch.listener;

import com.bioinformatics.dashboard.providers.postgres.gene.entity.ProteinEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class ImportUniprotSkipListenerTest {

    private ImportUniprotSkipListener listener;

    @BeforeEach
    void setUp() {
        listener = new ImportUniprotSkipListener();
    }

    @Test
    void onSkipInWrite_logsErrorWithoutThrowing() {
        ProteinEntry entry = new ProteinEntry();
        entry.setAccession("P12345");
        Throwable error = new RuntimeException("Database constraint violation");

        // We mainly verify that the logger doesn't throw and handles the object correctly
        assertThatCode(() -> listener.onSkipInWrite(entry, error))
                .doesNotThrowAnyException();
    }
}


package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.ProteinParsingContext;
import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.handler.IdentificationProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IdentificationProcessorTest {

    @Test
    void parsesIdReviewedAndLength() {
        var p = new IdentificationProcessor();
        var ctx = new ProteinParsingContext();
        p.process("PROT1 Reviewed; 123", ctx);
        var entry = ctx.getEntryBuilder().build();
        assertEquals("PROT1", entry.getEntryName());
        assertTrue(entry.getReviewed());
        assertEquals(123, entry.getLength());
    }
}


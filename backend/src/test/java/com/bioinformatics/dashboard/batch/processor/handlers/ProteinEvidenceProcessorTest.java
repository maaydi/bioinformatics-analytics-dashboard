package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.batch.processor.ProteinParsingContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProteinEvidenceProcessorTest {

    @Test
    void setsEvidenceLevel() {
        var p = new ProteinEvidenceProcessor();
        var ctx = new ProteinParsingContext();
        p.process("2", ctx);
        assertEquals((short) 2, ctx.getEntryBuilder().build().getEvidenceLevel());
    }
}


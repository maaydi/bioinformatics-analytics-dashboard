package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.ProteinParsingContext;
import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.handler.ProteinEvidenceProcessor;
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


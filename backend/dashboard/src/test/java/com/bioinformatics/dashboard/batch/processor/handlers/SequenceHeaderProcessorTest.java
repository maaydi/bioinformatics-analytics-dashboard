package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.ProteinParsingContext;
import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.handler.SequenceHeaderProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SequenceHeaderProcessorTest {

    @Test
    void parsesMwAndChecksumAndSequenceFlag() {
        var p = new SequenceHeaderProcessor();
        var ctx = new ProteinParsingContext();
        p.process("  12345 MW  ABCDEF CRC64", ctx);
        var entry = ctx.getEntryBuilder().build();
        assertEquals(12345, entry.getMolecularWeight());
        assertEquals("ABCDEF", entry.getSequenceChecksum());
        assertTrue(ctx.isInSequence());
    }
}


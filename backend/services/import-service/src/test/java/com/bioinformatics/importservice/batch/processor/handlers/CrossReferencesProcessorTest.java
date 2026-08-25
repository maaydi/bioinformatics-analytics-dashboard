package com.bioinformatics.importservice.batch.processor.handlers;

import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
import com.bioinformatics.importservice.uniprot.fileloader.processor.handler.CrossReferencesProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CrossReferencesProcessorTest {

    @Test
    void parsesCorrectly() {
        var p = new CrossReferencesProcessor();
        var ctx = new ProteinParsingContext();
        p.process("UniProtKB; P12345; Secondary; Info;", ctx);
        assertEquals(1, ctx.getCrossRefs().size());
        var cr = ctx.getCrossRefs().iterator().next();
        assertEquals("UniProtKB", cr.getSource());
        assertEquals("P12345", cr.getIdentifier());
        assertEquals("Secondary", cr.getSecondaryId());
        assertEquals("Info", cr.getTertiaryInfo());
    }
}


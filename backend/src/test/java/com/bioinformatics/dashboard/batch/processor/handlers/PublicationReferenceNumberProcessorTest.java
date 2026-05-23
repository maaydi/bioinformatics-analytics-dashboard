package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.batch.processor.ProteinParsingContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PublicationReferenceNumberProcessorTest {

    @Test
    void parsesRefNumber() {
        var rn = new PublicationReferenceNumberProcessor();
        var ctx = new ProteinParsingContext();
        rn.process("[3]", ctx);
        assertEquals(1, ctx.getPubBuilders().size());
        assertEquals((short) 3, ctx.getPubBuilders().get(0).build().getRefNumber());
    }
}


package com.bioinformatics.importservice.batch.processor.handlers;

import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
import com.bioinformatics.importservice.uniprot.fileloader.processor.handler.FeatureTableProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class FeatureTableProcessorTest {

    @Test
    void parsesFeatureAndAnnotations() {
        var p = new FeatureTableProcessor();
        var ctx = new ProteinParsingContext();
        p.process("DOMAIN         12..34", ctx);
        assertFalse(ctx.getFeatureBuilders().isEmpty());
        var fb = ctx.getFeatureBuilders().getLast();
        assertEquals("DOMAIN", fb.build().getFeatureType());
        assertEquals(12, fb.build().getStartPos());
        assertEquals(34, fb.build().getEndPos());

        p.process("/id=FT123", ctx);
        p.process("/note=important", ctx);
        p.process("/evidence=ECO:0000269", ctx);
        var built = ctx.getFeatureBuilders().getLast().build();
        assertEquals("FT123", built.getFeatureId());
        assertEquals("important", built.getNote());
        assertEquals("ECO:0000269", built.getEvidence());
    }
}


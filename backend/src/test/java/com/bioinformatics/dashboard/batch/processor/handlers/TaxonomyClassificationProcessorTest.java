package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.batch.processor.ProteinParsingContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaxonomyClassificationProcessorTest {

    @Test
    void parsesClassification() {
        var oc = new TaxonomyClassificationProcessor();
        var ctx = new ProteinParsingContext();
        oc.process("Bacteria; Proteobacteria; .;", ctx);
        assertTrue(ctx.getLineageAccum().contains("Bacteria"));
        assertTrue(ctx.getLineageAccum().contains("Proteobacteria"));
    }
}


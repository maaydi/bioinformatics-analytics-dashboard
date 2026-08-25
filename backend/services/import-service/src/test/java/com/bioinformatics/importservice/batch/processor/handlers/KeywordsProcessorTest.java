package com.bioinformatics.importservice.batch.processor.handlers;

import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
import com.bioinformatics.importservice.uniprot.fileloader.processor.handler.KeywordsProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KeywordsProcessorTest {

    @Test
    void collectsKeywords() {
        var p = new KeywordsProcessor();
        var ctx = new ProteinParsingContext();
        p.process("Kinase; Transferase; ", ctx);
        assertEquals(2, ctx.getKeywords().size());
    }
}


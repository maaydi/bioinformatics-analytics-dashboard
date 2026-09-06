package com.bioinformatics.importservice.batch.processor.handlers;

import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
import com.bioinformatics.importservice.uniprot.fileloader.processor.handler.TaxonomyIdProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TaxonomyIdProcessorTest {

    @Test
    void parsesTaxid() {
        var ox = new TaxonomyIdProcessor();
        var ctx = new ProteinParsingContext();
        ox.process("NCBI_TaxID=54321;", ctx);
        assertEquals(54321, ctx.getEntryBuilder().build().getTaxid());
    }
}


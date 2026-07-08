package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.ProteinParsingContext;
import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.handler.TaxonomyIdProcessor;
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


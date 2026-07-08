package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.ProteinParsingContext;
import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.handler.HostOrganismsProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class HostOrganismsProcessorTest {

    @Test
    void parsesNameAndTaxid() {
        var p = new HostOrganismsProcessor();
        var ctx = new ProteinParsingContext();
        p.process("OH   ; Homo sapiens; NCBI_TaxID=9606;", ctx);
        assertFalse(ctx.getHostOrganisms().isEmpty());
        var h = ctx.getHostOrganisms().iterator().next();
        assertEquals(9606, h.getTaxid());
        assertEquals("Homo sapiens", h.getName());
    }
}


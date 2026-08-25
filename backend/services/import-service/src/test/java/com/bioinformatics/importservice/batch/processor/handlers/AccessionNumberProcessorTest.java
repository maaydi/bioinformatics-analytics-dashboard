package com.bioinformatics.importservice.batch.processor.handlers;

import com.bioinformatics.importservice.resolver.ProteinAccessionResolver;
import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
import com.bioinformatics.importservice.uniprot.fileloader.processor.handler.AccessionNumberProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AccessionNumberProcessorTest {

    @Test
    void marksDuplicateAndSetsAccession() {
        var resolver = mock(ProteinAccessionResolver.class);
        when(resolver.alreadyExists("P12345")).thenReturn(true);
        var p = new AccessionNumberProcessor(resolver);
        var ctx = new ProteinParsingContext();
        p.process("P12345; other", ctx);
        assertTrue(ctx.isSkipEntry());
        assertEquals("P12345", ctx.getEntryBuilder().build().getAccession());
    }
}


package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.ProteinParsingContext;
import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.handler.GeneNameProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GeneNameProcessorTest {

    @Test
    void parsesNamesSynonymsOrf() {
        var p = new GeneNameProcessor();
        var ctx = new ProteinParsingContext();
        p.process("Name=gnA;", ctx);
        p.process("Synonyms= syn1;", ctx);
        p.process("ORFNames= orf1;", ctx);
        var entry = ctx.getEntryBuilder().build();
        assertEquals("gnA", entry.getGeneNamePrimary());
        assertArrayEquals(new String[]{"syn1"}, ctx.getSynonyms().toArray());
        assertArrayEquals(new String[]{"orf1"}, ctx.getOrfNames().toArray());
    }
}


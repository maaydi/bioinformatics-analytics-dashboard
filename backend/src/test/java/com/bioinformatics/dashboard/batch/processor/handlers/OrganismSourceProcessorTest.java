package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.batch.processor.ProteinParsingContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrganismSourceProcessorTest {

    @Test
    void setsOrganismName() {
        var p = new OrganismSourceProcessor();
        var ctx = new ProteinParsingContext();
        p.process("Homo sapiens.", ctx);
        assertEquals("Homo sapiens", ctx.getEntryBuilder().build().getOrganismName());
    }
}


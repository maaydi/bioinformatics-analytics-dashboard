package com.bioinformatics.importservice.batch.processor.handlers;

import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
import com.bioinformatics.importservice.uniprot.fileloader.processor.handler.OrganismSourceProcessor;
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


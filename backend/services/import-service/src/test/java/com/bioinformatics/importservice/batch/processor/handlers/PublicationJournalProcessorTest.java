package com.bioinformatics.importservice.batch.processor.handlers;

import com.bioinformatics.common.gene.entity.ProteinPublication;
import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
import com.bioinformatics.importservice.uniprot.fileloader.processor.handler.PublicationJournalProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PublicationJournalProcessorTest {

    @Test
    void appendsJournal() {
        var pubBuilder = ProteinPublication.builder().journal("Nat");
        var ctx = new ProteinParsingContext();
        ctx.getPubBuilders().add(pubBuilder);

        var rl = new PublicationJournalProcessor();
        rl.process("Journal of Tests", ctx);
        assertEquals("NatJournal of Tests", ctx.getPubBuilders().getLast().build().getJournal());
    }
}


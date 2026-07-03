package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.batch.processor.ProteinParsingContext;
import com.bioinformatics.dashboard.providers.postgres.gene.entity.ProteinPublication;
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


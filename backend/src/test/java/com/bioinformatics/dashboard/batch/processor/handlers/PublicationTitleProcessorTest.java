package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.batch.processor.ProteinParsingContext;
import com.bioinformatics.dashboard.gene.entity.ProteinPublication;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PublicationTitleProcessorTest {

    @Test
    void appendsTitle() {
        var pubBuilder = ProteinPublication.builder().title("Old");
        var ctx = new ProteinParsingContext();
        ctx.getPubBuilders().add(pubBuilder);

        var rt = new PublicationTitleProcessor();
        rt.process("A new title;", ctx);
        assertEquals("OldA new title", ctx.getPubBuilders().getLast().build().getTitle());
    }
}


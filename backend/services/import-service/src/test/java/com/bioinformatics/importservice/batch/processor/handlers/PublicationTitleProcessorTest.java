package com.bioinformatics.importservice.batch.processor.handlers;

import com.bioinformatics.common.gene.entity.ProteinPublication;
import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
import com.bioinformatics.importservice.uniprot.fileloader.processor.handler.PublicationTitleProcessor;
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


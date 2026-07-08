package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.ProteinParsingContext;
import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.handler.PublicationAuthorProcessor;
import com.bioinformatics.dashboard.providers.postgres.gene.entity.ProteinPublication;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PublicationAuthorProcessorTest {

    @Test
    void appendsAuthors() {
        var pubBuilder = ProteinPublication.builder().authors("Smith");
        var ctx = new ProteinParsingContext();
        ctx.getPubBuilders().add(pubBuilder);

        var ra = new PublicationAuthorProcessor();
        ra.process("Doe;", ctx);
        assertEquals("SmithDoe", ctx.getPubBuilders().getLast().build().getAuthors());
    }
}


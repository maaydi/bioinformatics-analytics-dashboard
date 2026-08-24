package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.common.gene.entity.ProteinPublication;
import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.ProteinParsingContext;
import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.handler.PublicationPubMedProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PublicationPubMedProcessorTest {

    @Test
    void setsPubmedAndDoi() {
        var pubBuilder = ProteinPublication.builder();
        var ctx = new ProteinParsingContext();
        ctx.getPubBuilders().add(pubBuilder);

        var rx = new PublicationPubMedProcessor();
        rx.process("PubMed=12345; DOI=10.1000/test;", ctx);
        assertEquals("12345", ctx.getPubBuilders().getLast().build().getPubmedId());
        assertEquals("10.1000/test", ctx.getPubBuilders().getLast().build().getDoi());
    }
}


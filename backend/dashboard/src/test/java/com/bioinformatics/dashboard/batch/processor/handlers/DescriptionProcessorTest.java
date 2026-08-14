package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.ProteinParsingContext;
import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.handler.DescriptionProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DescriptionProcessorTest {

    @Test
    void extractsNamesAndEc() {
        var p = new DescriptionProcessor();
        var ctx = new ProteinParsingContext();
        p.process("RecName: Full=Full Protein;", ctx);
        p.process("RecName: Short=ShortName;", ctx);
        p.process("EC=1.2.3.4;", ctx);
        var entry = ctx.getEntryBuilder().build();
        assertEquals("Full Protein", entry.getProteinFullName());
        assertEquals("ShortName", entry.getProteinShortName());
        assertEquals("1.2.3.4", entry.getProteinEcNumber());
    }
}


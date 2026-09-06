package com.bioinformatics.importservice.batch.processor.handlers;

import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
import com.bioinformatics.importservice.uniprot.fileloader.processor.handler.DateProcessor;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateProcessorTest {

    @Test
    void setsDatesAndVersions() {
        var p = new DateProcessor();
        var ctx = new ProteinParsingContext();
        p.process("01-JAN-2000, sequence version 2", ctx);
        var entry = ctx.getEntryBuilder().build();
        assertEquals(LocalDate.of(2000, 1, 1), entry.getSequenceDate());
        assertEquals((short) 2, entry.getSequenceVersion());

        p.process("02-FEB-2010, integrated", ctx);
        assertEquals(LocalDate.of(2010, 2, 2), ctx.getEntryBuilder().build().getIntegratedDate());

        p.process("03-MAR-2020, entry version 5", ctx);
        assertEquals((short) 5, ctx.getEntryBuilder().build().getEntryVersion());
    }
}


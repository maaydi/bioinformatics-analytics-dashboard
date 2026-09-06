package com.bioinformatics.importservice.batch.processor;

import com.bioinformatics.importservice.uniprot.fileloader.processor.LineProcessor;
import com.bioinformatics.importservice.uniprot.fileloader.processor.LineProcessorRegistry;
import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class LineProcessorRegistryTest {

    @Test
    void shouldIgnoreShortLines() {
        var p1 = mock(LineProcessor.class);
        when(p1.getPrefix()).thenReturn("AA");
        var registry = new LineProcessorRegistry(List.of(p1));
        clearInvocations(p1);
        var ctx = new ProteinParsingContext();
        registry.process("X", ctx);

        verifyNoInteractions(p1);
        assertEquals(0, ctx.getSequenceBuilder().length());
    }

    @Test
    void whenInSequence_andLineStartsWithDoubleSlash_thenDoNotAppend() {
        var p1 = mock(LineProcessor.class);
        when(p1.getPrefix()).thenReturn("AA");
        var registry = new LineProcessorRegistry(List.of(p1));
        clearInvocations(p1);
        var ctx = new ProteinParsingContext();
        ctx.setInSequence(true);

        var line = "//   comment in sequence"; // length >=5
        registry.process(line, ctx);

        assertEquals(0, ctx.getSequenceBuilder().length());
        verifyNoInteractions(p1);
    }

    @Test
    void whenInSequence_andLineIsSequence_thenAppendWithoutWhitespace() {
        var p1 = mock(LineProcessor.class);
        when(p1.getPrefix()).thenReturn("AA");
        var registry = new LineProcessorRegistry(List.of(p1));
        clearInvocations(p1);
        var ctx = new ProteinParsingContext();
        ctx.setInSequence(true);

        var line = "AA   M T G A"; // will be appended as "AAMTGA"
        registry.process(line, ctx);

        assertEquals("AAMTGA", ctx.getSequenceBuilder().toString());
        verifyNoInteractions(p1);
    }

    @Test
    void shouldInvokeProcessorWithTrimmedData() {
        var p = mock(LineProcessor.class);
        when(p.getPrefix()).thenReturn("AB");
        var registry = new LineProcessorRegistry(List.of(p));
        clearInvocations(p);
        var ctx = new ProteinParsingContext();
        var line = "AB   myData with spaces"; // substring(5) -> "myData with spaces"
        registry.process(line, ctx);

        verify(p, times(1)).process("myData with spaces", ctx);
    }

    @Test
    void shouldInvokeProcessorWithEmptyDataWhenLineLengthIsFive() {
        var p = mock(LineProcessor.class);
        when(p.getPrefix()).thenReturn("AB");
        var registry = new LineProcessorRegistry(List.of(p));
        clearInvocations(p);
        var ctx = new ProteinParsingContext();
        var line = "AB   "; // length == 5 -> substring(5) == ""
        assertEquals(5, line.length());
        registry.process(line, ctx);

        verify(p, times(1)).process("", ctx);
    }

    @Test
    void missingProcessor_shouldNotThrow() {
        var p = mock(LineProcessor.class);
        when(p.getPrefix()).thenReturn("AB");
        var registry = new LineProcessorRegistry(List.of(p));
        clearInvocations(p);
        var ctx = new ProteinParsingContext();
        var line = "ZZ   something"; // prefix ZZ not registered
        registry.process(line, ctx);

        verifyNoInteractions(p);
    }

    @Test
    void duplicatePrefixes_shouldFailConstruction() {
        var p1 = mock(LineProcessor.class);
        var p2 = mock(LineProcessor.class);
        when(p1.getPrefix()).thenReturn("DUP");
        when(p2.getPrefix()).thenReturn("DUP");

        assertThrows(IllegalStateException.class, () -> new LineProcessorRegistry(List.of(p1, p2)));
    }
}


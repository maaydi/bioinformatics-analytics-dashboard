package com.bioinformatics.importservice.batch.processor.handlers;

import com.bioinformatics.importservice.uniprot.fileloader.processor.ProteinParsingContext;
import com.bioinformatics.importservice.uniprot.fileloader.processor.handler.CommentsProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CommentsProcessorTest {

    @Test
    void parsesAndAppendsComments() {
        var p = new CommentsProcessor();
        var ctx = new ProteinParsingContext();

        p.process("CC -!- FUNCTION: Some function;", ctx);
        assertFalse(ctx.getCommBuilders().isEmpty());
        var built = ctx.getCommBuilders().getFirst().build();
        assertEquals("FUNCTION", built.getCommentType());
        assertEquals("Some function", built.getText());

        p.process("CC continuation text", ctx);
        var appended = ctx.getCommBuilders().getLast().build().getText();
        assertTrue(appended.contains("Some function"));
    }
}


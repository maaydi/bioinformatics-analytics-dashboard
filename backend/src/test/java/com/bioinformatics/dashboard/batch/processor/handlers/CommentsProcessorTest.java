package com.bioinformatics.dashboard.batch.processor.handlers;

import com.bioinformatics.dashboard.batch.processor.ProteinParsingContext;
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


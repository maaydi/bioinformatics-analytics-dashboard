package com.bioinformatics.dashboard.batch.processor;

import com.bioinformatics.dashboard.job.resolver.KeywordResolver;
import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.LineProcessorRegistry;
import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.ProteinEntryItemProcessor;
import com.bioinformatics.dashboard.job.uniprot.fileloader.processor.ProteinParsingContext;
import com.bioinformatics.dashboard.providers.postgres.gene.entity.ProteinEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProteinEntryItemProcessorTest {

    @Mock
    private KeywordResolver keywordResolver;

    @Mock
    private LineProcessorRegistry registry;

    @InjectMocks
    private ProteinEntryItemProcessor processor;

    @Test
    void process_emptyItem_returnsNull() {
        assertNull(processor.process("   "));
        assertNull(processor.process(""));
        verifyNoInteractions(registry, keywordResolver);
    }

    @Test
    void process_skipEntry_returnsNull() {
        String item = "ID   SOME_RECORD\nAC   P12345";

        doAnswer(invocation -> {
            ProteinParsingContext ctx = invocation.getArgument(1);
            ctx.setSkipEntry(true);
            return null;
        }).when(registry).process(anyString(), any(ProteinParsingContext.class));

        assertNull(processor.process(item));
        verify(keywordResolver, never()).resolveKeywords(any());
    }

    @Test
    void process_validItem_returnsProteinEntry() {
        String item = "ID   SOME_RECORD\nAC   P12345";

        doAnswer(invocation -> {
            ProteinParsingContext ctx = invocation.getArgument(1);
            ctx.getEntryBuilder().entryName("SOME_RECORD");
            ctx.getEntryBuilder().accession("P12345");
            return null;
        }).when(registry).process(anyString(), any(ProteinParsingContext.class));

        when(keywordResolver.resolveKeywords(any())).thenReturn(List.of());

        ProteinEntry result = processor.process(item);

        assertNotNull(result);
        verify(registry, times(2)).process(anyString(), any(ProteinParsingContext.class));
        verify(keywordResolver).resolveKeywords(any());
    }
}

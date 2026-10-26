package com.bioinformatics.common.batch.reader;

import com.bioinformatics.common.gene.entity.ProteinEntry;
import com.bioinformatics.common.gene.service.ProteinEntryService;
import com.bioinformatics.common.models.gene.GeneSearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UniProtPostgresItemReaderTest {

    private static final String PAGE_NUMBER = "uniProtPostgresItemReader.pageNumber";
    private static final String PAGE_COUNT_KEY = "uniProtPostgresItemReader.pageCount";
    private static final int PAGE_SIZE = 2;

    @Mock
    private ProteinEntryService proteinEntryService;

    private UniProtPostgresItemReader reader;

    @BeforeEach
    void setUp() {
        var request = GeneSearchRequest.builder().page(0).size(PAGE_SIZE).build();
        reader = new UniProtPostgresItemReader(proteinEntryService, request, PAGE_SIZE);
    }

    @Test
    void freshReader_checkpointsPageZeroBeforeFetching() {
        assertCheckpoint(0, 0);
        verifyNoInteractions(proteinEntryService);
    }

    @Test
    void openWithoutCheckpoint_readsPageZeroAndCheckpointsNextPage() {
        // Arrange
        var entry = new ProteinEntry();
        var firstPage = PageRequest.of(0, PAGE_SIZE);
        when(proteinEntryService.findAll(any(), eq(firstPage)))
                .thenReturn(new PageImpl<>(List.of(entry), firstPage, 1));

        // Act
        reader.open(new ExecutionContext());

        // Assert
        assertCheckpoint(0, 0);
        verifyNoInteractions(proteinEntryService);
        assertThat(reader.read()).isSameAs(entry);
        assertCheckpoint(1, 1);
        verify(proteinEntryService).findAll(any(), eq(firstPage));
        verifyNoMoreInteractions(proteinEntryService);
    }

    @Test
    void read_consumesBufferedEntriesBeforeFetchingNextPageAndStopsAtExhaustion() {
        // Arrange
        var first = new ProteinEntry();
        var second = new ProteinEntry();
        var third = new ProteinEntry();
        var firstPage = PageRequest.of(0, PAGE_SIZE);
        var secondPage = PageRequest.of(1, PAGE_SIZE);
        when(proteinEntryService.findAll(any(), eq(firstPage)))
                .thenReturn(new PageImpl<>(List.of(first, second), firstPage, 3));
        when(proteinEntryService.findAll(any(), eq(secondPage)))
                .thenReturn(new PageImpl<>(List.of(third), secondPage, 3));
        reader.open(new ExecutionContext());

        // Act & Assert
        assertThat(reader.read()).isSameAs(first);
        assertThat(reader.read()).isSameAs(second);
        assertCheckpoint(1, 1);
        verify(proteinEntryService).findAll(any(), eq(firstPage));
        verifyNoMoreInteractions(proteinEntryService);

        assertThat(reader.read()).isSameAs(third);
        assertThat(reader.read()).isNull();
        assertThat(reader.read()).isNull();
        assertCheckpoint(2, 2);
        verify(proteinEntryService).findAll(any(), eq(secondPage));
        verifyNoMoreInteractions(proteinEntryService);
    }

    @Test
    void read_emptyFirstPageExhaustsReaderWithoutRefetching() {
        // Arrange
        var firstPage = PageRequest.of(0, PAGE_SIZE);
        when(proteinEntryService.findAll(any(), eq(firstPage))).thenReturn(Page.empty(firstPage));
        reader.open(new ExecutionContext());

        // Act & Assert
        assertThat(reader.read()).isNull();
        assertThat(reader.read()).isNull();
        assertCheckpoint(1, 1);
        verify(proteinEntryService).findAll(any(), eq(firstPage));
        verifyNoMoreInteractions(proteinEntryService);
    }

    @Test
    void openWithCheckpoint_restoresNextPageAndPageCount() {
        // Arrange
        var context = new ExecutionContext();
        context.putInt(PAGE_NUMBER, 4);
        context.putInt(PAGE_COUNT_KEY, 7);
        var entry = new ProteinEntry();
        var savedPage = PageRequest.of(4, PAGE_SIZE);
        when(proteinEntryService.findAll(any(), eq(savedPage)))
                .thenReturn(new PageImpl<>(List.of(entry), savedPage, 9));

        // Act
        reader.open(context);

        // Assert
        assertCheckpoint(4, 7);
        assertThat(reader.read()).isSameAs(entry);
        reader.update(context);
        assertThat(context.getInt(PAGE_NUMBER)).isEqualTo(5);
        assertThat(context.getInt(PAGE_COUNT_KEY)).isEqualTo(8);
        verify(proteinEntryService).findAll(any(), eq(savedPage));
        verifyNoMoreInteractions(proteinEntryService);
    }

    @Test
    void openWithSavedPageButNoPageCount_defaultsCountToZero() {
        // Arrange
        var context = new ExecutionContext();
        context.putInt(PAGE_NUMBER, 4);
        var savedPage = PageRequest.of(4, PAGE_SIZE);
        when(proteinEntryService.findAll(any(), eq(savedPage))).thenReturn(Page.empty(savedPage));

        // Act
        reader.open(context);

        // Assert
        assertCheckpoint(4, 0);
        assertThat(reader.read()).isNull();
        assertCheckpoint(5, 1);
        verify(proteinEntryService).findAll(any(), eq(savedPage));
        verifyNoMoreInteractions(proteinEntryService);
    }

    @Test
    void read_failedFetchDoesNotAdvancePageOrPageCountAndCanBeRetried() {
        // Arrange
        var context = new ExecutionContext();
        context.putInt(PAGE_NUMBER, 4);
        context.putInt(PAGE_COUNT_KEY, 7);
        var savedPage = PageRequest.of(4, PAGE_SIZE);
        var failure = new DataAccessResourceFailureException("Database unavailable");
        var entry = new ProteinEntry();
        when(proteinEntryService.findAll(any(), eq(savedPage)))
                .thenThrow(failure)
                .thenReturn(new PageImpl<>(List.of(entry), savedPage, 9));
        reader.open(context);

        // Act & Assert
        assertThatThrownBy(reader::read).isSameAs(failure);
        assertCheckpoint(4, 7);
        assertThat(reader.read()).isSameAs(entry);
        assertCheckpoint(5, 8);
        verify(proteinEntryService, times(2)).findAll(any(), eq(savedPage));
        verifyNoMoreInteractions(proteinEntryService);
    }

    @Test
    void reopenWithoutCheckpoint_resetsPageCountAndExhaustion() {
        // Arrange
        var entry = new ProteinEntry();
        var firstPage = PageRequest.of(0, PAGE_SIZE);
        when(proteinEntryService.findAll(any(), eq(firstPage)))
                .thenReturn(new PageImpl<>(List.of(entry), firstPage, 1));
        reader.open(new ExecutionContext());
        assertThat(reader.read()).isSameAs(entry);
        assertThat(reader.read()).isNull();
        assertCheckpoint(1, 1);
        reader.close();

        // Act
        reader.open(new ExecutionContext());

        // Assert
        assertCheckpoint(0, 0);
        assertThat(reader.read()).isSameAs(entry);
        assertCheckpoint(1, 1);
        verify(proteinEntryService, times(2)).findAll(any(), eq(firstPage));
        verifyNoMoreInteractions(proteinEntryService);
    }

    @Test
    void reopenWithCheckpoint_discardsBufferedEntriesAndResetsExhaustion() {
        // Arrange
        var first = new ProteinEntry();
        var buffered = new ProteinEntry();
        var resumed = new ProteinEntry();
        var firstPage = PageRequest.of(0, PAGE_SIZE);
        var savedPage = PageRequest.of(3, PAGE_SIZE);
        when(proteinEntryService.findAll(any(), eq(firstPage)))
                .thenReturn(new PageImpl<>(List.of(first, buffered), firstPage, 2));
        when(proteinEntryService.findAll(any(), eq(savedPage)))
                .thenReturn(new PageImpl<>(List.of(resumed), savedPage, 7));
        reader.open(new ExecutionContext());
        assertThat(reader.read()).isSameAs(first);
        var context = new ExecutionContext();
        context.putInt(PAGE_NUMBER, 3);
        context.putInt(PAGE_COUNT_KEY, 6);

        // Act
        reader.open(context);

        // Assert
        assertCheckpoint(3, 6);
        assertThat(reader.read()).isSameAs(resumed);
        assertThat(reader.read()).isNull();
        assertCheckpoint(4, 7);
        verify(proteinEntryService).findAll(any(), eq(firstPage));
        verify(proteinEntryService).findAll(any(), eq(savedPage));
        verifyNoMoreInteractions(proteinEntryService);
    }

    private void assertCheckpoint(int nextPage, int pageCount) {
        var context = new ExecutionContext();
        reader.update(context);
        assertThat(context.getInt(PAGE_NUMBER)).isEqualTo(nextPage);
        assertThat(context.getInt(PAGE_COUNT_KEY)).isEqualTo(pageCount);
    }
}

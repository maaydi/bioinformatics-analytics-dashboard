package com.bioinformatics.importservice.batch.counter;

import com.bioinformatics.common.exception.UnsupportedFileTypeException;
import com.bioinformatics.importservice.uniprot.fileloader.counter.CounterRegistry;
import com.bioinformatics.importservice.uniprot.fileloader.counter.RecordCounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CounterRegistryTest {

    @Mock
    private RecordCounter datCounter;

    @Mock
    private RecordCounter tsvCounter;

    private CounterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new CounterRegistry(List.of(datCounter, tsvCounter));
    }

    @Test
    void getCounter_ShouldReturnSupportedCounter() {
        // Arrange
        String filename = "test.dat";
        given(datCounter.supports(filename)).willReturn(true);

        // Act
        RecordCounter result = registry.getCounter(filename);

        // Assert
        assertThat(result).isSameAs(datCounter);
    }

    @Test
    void getCounter_ShouldReturnFirstSupportedCounter() {
        // Arrange
        String filename = "test.tsv";
        given(datCounter.supports(filename)).willReturn(false);
        given(tsvCounter.supports(filename)).willReturn(true);

        // Act
        RecordCounter result = registry.getCounter(filename);

        // Assert
        assertThat(result).isSameAs(tsvCounter);
    }

    @Test
    void getCounter_ShouldThrowException_WhenNoCounterSupportsFile() {
        // Arrange
        String filename = "test.txt";
        given(datCounter.supports(filename)).willReturn(false);
        given(tsvCounter.supports(filename)).willReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> registry.getCounter(filename))
                .isInstanceOf(UnsupportedFileTypeException.class)
                .hasMessageContaining("No counter found for file: " + filename);
    }
}


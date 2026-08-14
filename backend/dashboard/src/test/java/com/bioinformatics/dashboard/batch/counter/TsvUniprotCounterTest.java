package com.bioinformatics.dashboard.batch.counter;

import com.bioinformatics.dashboard.job.uniprot.fileloader.counter.TsvUniprotCounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class TsvUniprotCounterTest {

    private TsvUniprotCounter counter;

    @BeforeEach
    void setUp() {
        counter = new TsvUniprotCounter();
    }

    @Test
    void supports_ShouldReturnTrue_WhenExtensionIsTsv() {
        assertThat(counter.supports("test.tsv")).isTrue();
        assertThat(counter.supports("TEST.TSV")).isTrue();
        assertThat(counter.supports("path/to/file.tSv")).isTrue();
    }

    @Test
    void supports_ShouldReturnFalse_WhenExtensionIsNotTsv() {
        assertThat(counter.supports("test.dat")).isFalse();
        assertThat(counter.supports("test.csv")).isFalse();
        assertThat(counter.supports(null)).isFalse();
        assertThat(counter.supports("")).isFalse();
    }

    @Test
    void count_ShouldReturnNumberOfNonBlankLines() throws IOException {
        String data = """
                Entry\tEntry Name\tStatus
                P00004\tCYC_MACMQ\treviewed
                
                P00005\tCYC_MACFA\treviewed
                
                P00006\tTEST\treviewed
                """;

        try (var inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))) {
            long result = counter.count(inputStream);
            // 1 header + 3 valid lines = 4 non-blank lines. Remaining lines are composed of whitespaces or are empty.
            assertThat(result).isEqualTo(4L);
        }
    }

    @Test
    void count_ShouldReturnZeroForEmptyStream() throws IOException {
        String data = "";

        try (var inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))) {
            long result = counter.count(inputStream);
            assertThat(result).isZero();
        }
    }
}


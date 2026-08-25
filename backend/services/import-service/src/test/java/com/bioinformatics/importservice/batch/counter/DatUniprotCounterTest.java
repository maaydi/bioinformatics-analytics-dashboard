package com.bioinformatics.importservice.batch.counter;

import com.bioinformatics.importservice.uniprot.fileloader.counter.DatUniprotCounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DatUniprotCounterTest {

    private DatUniprotCounter counter;

    @BeforeEach
    void setUp() {
        counter = new DatUniprotCounter();
    }

    @Test
    void supports_ShouldReturnTrue_WhenExtensionIsDat() {
        assertThat(counter.supports("test.dat")).isTrue();
        assertThat(counter.supports("TEST.DAT")).isTrue();
        assertThat(counter.supports("path/to/file.dAt")).isTrue();
    }

    @Test
    void supports_ShouldReturnFalse_WhenExtensionIsNotDat() {
        assertThat(counter.supports("test.tsv")).isFalse();
        assertThat(counter.supports("test.txt")).isFalse();
        assertThat(counter.supports(null)).isFalse();
        assertThat(counter.supports("")).isFalse();
    }

    @Test
    void count_ShouldReturnNumberOfRecords() throws IOException {
        String data = """
                ID   CYC_MACMQ              Reviewed;         104 AA.
                AC   P00004;
                DE   RecName: Full=Cytochrome c;
                //
                ID   CYC_MACFA              Reviewed;         104 AA.
                //
                ID   SOMETHING
                """;

        try (var inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))) {
            long result = counter.count(inputStream);
            assertThat(result).isEqualTo(2L);
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

    @Test
    void count_ShouldIgnoreWhitespaceAroundDelimiter() throws IOException {
        String data = "ID  ONE\n  //  \nID  TWO\n//\n";

        try (var inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))) {
            long result = counter.count(inputStream);
            assertThat(result).isEqualTo(2L);
        }
    }
}


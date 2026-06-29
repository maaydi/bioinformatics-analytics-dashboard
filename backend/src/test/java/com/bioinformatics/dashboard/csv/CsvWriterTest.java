package com.bioinformatics.dashboard.csv;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvWriterTest {

    private final CsvWriter csvWriter = new CsvWriter();

    @Test
    void write_ShouldWriteNothing_WhenListIsNull() throws IOException {
        StringWriter writer = new StringWriter();
        csvWriter.write(writer, null);
        assertThat(writer.toString()).isEmpty();
    }

    @Test
    void write_ShouldWriteNothing_WhenListIsEmpty() throws IOException {
        StringWriter writer = new StringWriter();
        csvWriter.write(writer, Collections.emptyList());
        assertThat(writer.toString()).isEmpty();
    }

    @Test
    void write_ShouldWriteHeaderAndRows_WhenListHasElements() throws IOException {
        StringWriter writer = new StringWriter();
        List<DummyEntity> items = List.of(
                new DummyEntity("Alice", 30),
                new DummyEntity("Bob", 25)
        );

        csvWriter.write(writer, items);

        String result = writer.toString();
        String[] lines = result.split("\n");

        assertThat(lines).hasSize(3);
        assertThat(lines[0]).isEqualTo("name,age");
        assertThat(lines[1]).isEqualTo("\"Alice\",\"30\"");
        assertThat(lines[2]).isEqualTo("\"Bob\",\"25\"");
    }

    @Test
    void write_ShouldThrowException_WhenWriterFails() {
        java.io.Writer badWriter = new java.io.Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("Write failed");
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        List<DummyEntity> items = List.of(new DummyEntity("Alice", 30));
        assertThatThrownBy(() -> csvWriter.write(badWriter, items))
                .isInstanceOf(IOException.class)
                .hasMessage("Write failed");
    }

    private record DummyEntity(String name, int age) implements CsvSerializable {
        @Override
        public String row() {
            return format(name) + separator() + format(age);
        }
    }
}


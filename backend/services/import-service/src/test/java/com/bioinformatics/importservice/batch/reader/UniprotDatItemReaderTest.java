package com.bioinformatics.importservice.batch.reader;

import com.bioinformatics.importservice.uniprot.fileloader.reader.UniprotDatItemReader;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

class UniprotDatItemReaderTest {

    @Test
    void shouldReadSingleUniprotRecord() throws Exception {
        String data = """
                ID   104K_THEPA              Reviewed;         924 AA.
                AC   P15711;
                //
                """;
        Resource resource = new ByteArrayResource(data.getBytes());
        UniprotDatItemReader reader = new UniprotDatItemReader(resource);
        reader.open(new ExecutionContext());

        String record = reader.read();
        assertThat(record).isNotNull();
        assertThat(record).contains("ID   104K_THEPA");
        assertThat(record).contains("//\n");

        String record2 = reader.read();
        assertThat(record2).isNull();

        reader.close();
    }

    @Test
    void shouldReadMultipleUniprotRecords() throws Exception {
        String data = """
                ID   REC1
                AC   1
                //
                ID   REC2
                AC   2
                //
                """;
        Resource resource = new ByteArrayResource(data.getBytes());
        UniprotDatItemReader reader = new UniprotDatItemReader(resource);
        reader.open(new ExecutionContext());

        String record1 = reader.read();
        assertThat(record1).contains("ID   REC1");

        String record2 = reader.read();
        assertThat(record2).contains("ID   REC2");

        String record3 = reader.read();
        assertThat(record3).isNull();

        reader.close();
    }

    @Test
    void shouldHandleEmptyFile() throws Exception {
        Resource resource = new ByteArrayResource(new byte[0]);
        UniprotDatItemReader reader = new UniprotDatItemReader(resource);
        reader.open(new ExecutionContext());

        String record = reader.read();
        assertThat(record).isNull();

        reader.close();
    }
}


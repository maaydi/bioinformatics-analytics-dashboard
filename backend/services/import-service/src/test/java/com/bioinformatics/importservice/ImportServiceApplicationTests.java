package com.bioinformatics.importservice;

import com.bioinformatics.importservice.uniprot.apiloader.UniProtApiImportJobExecutor;
import com.bioinformatics.importservice.uniprot.fileloader.AsyncUniprotImportJobExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ImportServiceApplicationTests {
    @MockitoBean
    AsyncUniprotImportJobExecutor asyncUniprotImportJobExecutor;

    @MockitoBean
    private UniProtApiImportJobExecutor uniProtApiImportJobExecutor;

    @Test
    void contextLoads() {
    }

}

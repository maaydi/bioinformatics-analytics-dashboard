package com.bioinformatics.importservice;

import com.bioinformatics.importservice.uniprot.ImportJobExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ImportServiceApplicationTests {
    @MockitoBean
    ImportJobExecutor importJobExecutor;

    @Test
    void contextLoads() {
    }

}

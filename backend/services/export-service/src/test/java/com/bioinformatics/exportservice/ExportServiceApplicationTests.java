package com.bioinformatics.exportservice;

import com.bioinformatics.exportservice.batch.ExportJobExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ExportServiceApplicationTests {
    @MockitoBean
    ExportJobExecutor executor;

    @Test
    void contextLoads() {
    }

}

package com.bioinformatics.exportservice.batch;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

import static com.bioinformatics.exportservice.dto.Constants.DATA_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;

class ExportSourceDeciderTest {

    private final ExportSourceDecider decider = new ExportSourceDecider();

    @Test
    void decide_returnsLowercaseConfiguredProvider() {
        var execution = jobExecution(new JobParametersBuilder()
                .addString(DATA_PROVIDER.getKey(), "postgres")
                .toJobParameters());

        assertThat(decider.decide(execution, null).getName()).isEqualTo("postgres");
    }

    @Test
    void decide_returnsUnknownWhenProviderIsMissing() {
        var execution = jobExecution(new JobParameters());

        assertThat(decider.decide(execution, null).getName()).isEqualTo("UNKNOWN");
    }

    private JobExecution jobExecution(JobParameters parameters) {
        return new JobExecution(1L, new JobInstance(1L, "exportJob"), parameters);
    }
}


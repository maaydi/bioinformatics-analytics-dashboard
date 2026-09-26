package com.bioinformatics.exportservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ExportJobStepTrackerTest {

    @Mock
    JobInstance jobInstance;
    @Mock
    JobParameters jobParameters;
    private JobExecution jobExecution;

    @BeforeEach
    void setUp() {
        jobExecution = new JobExecution(100L, jobInstance, jobParameters);
    }

    @Test
    void shouldReturnStarting_whenNoStepsExistYet() {
        // No StepExecutions added to JobExecution
        String currentStep = ExportJobStepTracker.determineCurrentStep(jobExecution);
        assertEquals("STARTING", currentStep);
    }

    @Test
    void shouldReturnRunningStep_whenMiddleStepIsRunning() {
        jobExecution.addStepExecutions(List.of(
                createStep(1L, "validateAndEstimateStep", BatchStatus.COMPLETED),
                createStep(2L, "exportChunkStep", BatchStatus.STARTED)
        ));

        String currentStep = ExportJobStepTracker.determineCurrentStep(jobExecution);
        assertEquals("exportChunkStep", currentStep);
    }

    @Test
    void shouldReturnFailedStep_whenJobFailedAndNothingIsRunning() {
        jobExecution.addStepExecutions(List.of(
                createStep(1L, "validateAndEstimateStep", BatchStatus.COMPLETED),
                createStep(2L, "exportChunkStep", BatchStatus.FAILED)
        ));

        String currentStep = ExportJobStepTracker.determineCurrentStep(jobExecution);
        assertEquals("exportChunkStep", currentStep);
    }

    @Test
    void shouldReturnLastStep_whenJobIsFullyCompleted() {
        jobExecution.addStepExecutions(List.of(
                createStep(1L, "validateAndEstimateStep", BatchStatus.COMPLETED),
                createStep(2L, "exportChunkStep", BatchStatus.COMPLETED),
                createStep(3L, "assembleAndFinalizeStep", BatchStatus.COMPLETED)
        ));

        String currentStep = ExportJobStepTracker.determineCurrentStep(jobExecution);
        assertEquals("assembleAndFinalizeStep", currentStep);
    }

    @Test
    void shouldReturnHighestIdStep_whenJobRestartedAndFailedAgain() {
        // Scenario: Step 2 failed (ID 2). Job restarted, Step 2 ran again (ID 3) and failed again.
        jobExecution.addStepExecutions(List.of(
                createStep(1L, "validateAndEstimateStep", BatchStatus.COMPLETED),
                createStep(2L, "exportChunkStep", BatchStatus.FAILED),
                createStep(3L, "exportChunkStep", BatchStatus.FAILED)
        ));

        String currentStep = ExportJobStepTracker.determineCurrentStep(jobExecution);
        assertEquals("exportChunkStep", currentStep);
    }

    @Test
    void shouldReturnUnknown_whenJobExecutionIsNull() {
        String currentStep = ExportJobStepTracker.determineCurrentStep(null);
        assertEquals("UNKNOWN", currentStep);
    }

    private StepExecution createStep(long id, String stepName, BatchStatus status) {
        var step = new StepExecution(id, stepName, jobExecution);
        step.setStatus(status);
        return step;
    }
}

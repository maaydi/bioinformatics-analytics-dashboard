package com.bioinformatics.exportservice.service;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;

import java.util.Comparator;

public class ExportJobStepTracker {
    private static final String NO_EXECUTION_START = "UNKNOWN";

    public static String determineCurrentStep(JobExecution execution) {
        if (execution == null) {
            return NO_EXECUTION_START;
        }
        var steps = execution.getStepExecutions();
        if (steps.isEmpty()) {
            return "STARTING";
        }

        var runningStep = steps.stream()
                .filter(e -> e.getStatus().isRunning())
                .findFirst();
        return runningStep
                .map(StepExecution::getStepName)
                .orElseGet(() -> steps.stream()
                        .max(Comparator.comparing(StepExecution::getId))
                        .map(StepExecution::getStepName)
                        .orElse(NO_EXECUTION_START));
    }
}

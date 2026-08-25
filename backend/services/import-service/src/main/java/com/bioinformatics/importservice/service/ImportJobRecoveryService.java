package com.bioinformatics.importservice.service;

import com.bioinformatics.importservice.dto.ImportStatus;
import com.bioinformatics.importservice.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportJobRecoveryService {
    private final ImportJobRepository importJobRepo;

    @Transactional
    public void markImportJobAsFailed(UUID jobId) {
        importJobRepo.findById(jobId).ifPresentOrElse(
                importJob -> {
                    importJob.setStatus(ImportStatus.FAILED);
                    importJobRepo.save(importJob);
                },
                () -> log.error("Failed to update status: Import job with ID {} not found", jobId)
        );
    }
}

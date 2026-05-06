package com.bioinformatics.dashboard.admin.service;

import com.bioinformatics.dashboard.batch.AsyncUniprotImportJobExecutor;
import com.bioinformatics.dashboard.config.AppProperties;
import com.bioinformatics.dashboard.gene.dto.PagedResponse;
import com.bioinformatics.dashboard.job.dto.Constants;
import com.bioinformatics.dashboard.job.dto.ImportJobProgress;
import com.bioinformatics.dashboard.job.dto.ImportJobSummary;
import com.bioinformatics.dashboard.job.dto.ImportStatus;
import com.bioinformatics.dashboard.job.entity.ImportJob;
import com.bioinformatics.dashboard.job.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final ImportJobRepository importJobRep;
    private final AsyncUniprotImportJobExecutor importExec;
    private final AppProperties appProperties;

    public PagedResponse<ImportJobSummary> listImportJobs(int page, int size) {
        return new PagedResponse<>(List.of(
                new ImportJobSummary(
                        "job-8f3a2c91-4d7e-4b2a-9c11-6a9f5e2b7c33",
                        ImportStatus.COMPLETED,
                        "users_2026_05_02.csv",
                        15432,
                        2875L,
                        Instant.now(),
                        Instant.now(),
                        null)),
                1, 50, 1, 1);
    }

    @Transactional
    public ImportJobSummary triggerImport(MultipartFile file, String strategy) {
        try {
            var uploadDir = Paths.get(appProperties.getImportConfig().getTempDir());
            Files.createDirectories(uploadDir);
            var fname = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            var target = uploadDir.resolve(fname);
            if ("overwrite".equalsIgnoreCase(strategy)) {
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                fname = UUID.randomUUID() + "_" + fname;
                target = uploadDir.resolve(fname);
                Files.copy(file.getInputStream(), target);
            }

            var job = new ImportJob();
            job.setStatus(ImportStatus.RUNNING);
            job.setFileName(target.getFileName().toString());
            job.setStrategy(strategy.toUpperCase());
            var savedJob = importJobRep.save(job);

            var parameters = new JobParametersBuilder()
                    .addString(Constants.IMPORT_JOB_ID.name(), savedJob.getId().toString())
                    .addString(Constants.FILE_PATH.name(), target.toAbsolutePath().toString())
                    .addLong(Constants.TIMESTAMP.name(), System.currentTimeMillis())
                    .toJobParameters();

            importExec.execute(parameters);

            return new ImportJobSummary(
                    savedJob.getId().toString(),
                    ImportStatus.RUNNING,
                    target.getFileName().toString(),
                    0, 0L, savedJob.getCreatedAt(), null, null);

        } catch (Exception e) {
            throw new RuntimeException("Failed to trigger import " + e.getMessage(), e);
        }
    }

    public ImportJobProgress getImportJobStatus(String jobId) {
        throw new UnsupportedOperationException("Unimplemented method 'getImportJobStatus'");
    }

}

package com.bioinformatics.dashboard.admin.service;

import com.bioinformatics.dashboard.batch.AsyncUniprotImportJobExecutor;
import com.bioinformatics.dashboard.batch.counter.CounterRegistry;
import com.bioinformatics.dashboard.config.AppProperties;
import com.bioinformatics.dashboard.exception.ExecuteJobException;
import com.bioinformatics.dashboard.exception.ImportAlreadyRunningException;
import com.bioinformatics.dashboard.exception.MalformedUniprotFileException;
import com.bioinformatics.dashboard.gene.dto.PagedResponse;
import com.bioinformatics.dashboard.job.dto.Constants;
import com.bioinformatics.dashboard.job.dto.ImportJobProgress;
import com.bioinformatics.dashboard.job.dto.ImportJobSummary;
import com.bioinformatics.dashboard.job.dto.ImportStatus;
import com.bioinformatics.dashboard.job.entity.ImportJob;
import com.bioinformatics.dashboard.job.mapper.ImportJobMapper;
import com.bioinformatics.dashboard.job.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final ImportJobRepository importJobRep;
    private final ImportJobMapper jobMapper;
    private final AsyncUniprotImportJobExecutor importExec;
    private final AppProperties appProperties;
    private final CounterRegistry registry;

    public PagedResponse<ImportJobSummary> listImportJobs(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var summaryPage = importJobRep.findAll(pageable).map(jobMapper::toSummary);

        return new PagedResponse<>(summaryPage.getContent(),
                summaryPage.getNumber(),
                summaryPage.getSize(),
                summaryPage.getTotalElements(),
                summaryPage.getTotalPages());
    }

    @Transactional
    public ImportJobSummary triggerImport(MultipartFile file, String strategy) {
        checkImportAlreadyRunning();
        try {
            var target = saveImportFile(file, strategy);
            var savedJob = saveJob(target, strategy);
            executeImport(savedJob, target);
            return savedJob;
        } catch (Exception e) {
            throw new ExecuteJobException("Failed to trigger import " + e.getMessage(), e);
        }
    }

    public ImportJobProgress getImportJobStatus(String jobId) {
        var job = importJobRep.findById(UUID.fromString(jobId));
        if (job.isEmpty()) {
            return new ImportJobProgress(jobId, ImportStatus.FAILED, "", 0, 0, 0, 0L,
                    "Could not find job with ID <%s>".formatted(jobId));
        }
        return jobMapper.toJobProgress(job.get());
    }

    private void checkImportAlreadyRunning() {
        var running = importJobRep.findByStatus(ImportStatus.RUNNING);
        if (!running.isEmpty()) {
            throw new ImportAlreadyRunningException(running.getFirst().getId().toString());
        }

    }

    private Path saveImportFile(MultipartFile file, String strategy) throws IOException {
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
        return target;
    }

    private ImportJobSummary saveJob(Path file, String strategy) {
        var totalRecords = countUniprotEntries(file);
        var job = new ImportJob();
        job.setStatus(ImportStatus.RUNNING);
        job.setFileName(file.getFileName().toString());
        job.setStrategy(strategy.toUpperCase());
        job.setTotalEstimated(totalRecords);
        var savedJob = importJobRep.save(job);
        return jobMapper.toSummary(savedJob);
    }

    private int countUniprotEntries(Path file) {
        var counter = registry.getCounter(file.toString());
        try (var is = Files.newInputStream(file)) {
            return (int) counter.count(is);
        } catch (Exception e) {
            throw new MalformedUniprotFileException(e.getMessage());
        }
    }

    private void executeImport(ImportJobSummary importJob, Path file) {
        var parameters = new JobParametersBuilder()
                .addString(Constants.IMPORT_JOB_ID.getKey(), importJob.id())
                .addString(Constants.FILE_PATH.getKey(), file.toAbsolutePath().toString())
                .addLong(Constants.TIMESTAMP.getKey(), System.currentTimeMillis())
                .toJobParameters();
        importExec.execute(parameters);
    }


}

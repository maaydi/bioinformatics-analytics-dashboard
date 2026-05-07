package com.bioinformatics.dashboard.admin.service;

import com.bioinformatics.dashboard.batch.AsyncUniprotImportJobExecutor;
import com.bioinformatics.dashboard.batch.counter.CounterRegistry;
import com.bioinformatics.dashboard.config.AppProperties;
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

            var totalRecords = countUniprotEntries(target);
            var job = new ImportJob();
            job.setStatus(ImportStatus.RUNNING);
            job.setFileName(target.getFileName().toString());
            job.setStrategy(strategy.toUpperCase());
            job.setTotalEstimated(totalRecords);
            var savedJob = importJobRep.save(job);

            var parameters = new JobParametersBuilder()
                    .addString(Constants.IMPORT_JOB_ID.getKey(), savedJob.getId().toString())
                    .addString(Constants.FILE_PATH.getKey(), target.toAbsolutePath().toString())
                    .addLong(Constants.TIMESTAMP.getKey(), System.currentTimeMillis())
                    .toJobParameters();

            importExec.execute(parameters);

            return new ImportJobSummary(
                    savedJob.getId().toString(),
                    ImportStatus.RUNNING,
                    target.getFileName().toString(), 0,
                    totalRecords, 0L, savedJob.getCreatedAt(), null, null);

        } catch (Exception e) {
            throw new RuntimeException("Failed to trigger import " + e.getMessage(), e);
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

    private int countUniprotEntries(Path file) {
        var counter = registry.getCounter(file.toString());
        try (var is = Files.newInputStream(file)) {
            return (int) counter.count(is);
        } catch (Exception e) {
            throw new MalformedUniprotFileException(e.getMessage());
        }
    }

}

package com.bioinformatics.dashboard.admin.service;

import com.bioinformatics.common.exception.ExecuteJobException;
import com.bioinformatics.common.exception.ImportAlreadyRunningException;
import com.bioinformatics.common.exception.MalformedUniprotFileException;
import com.bioinformatics.common.exception.ResourceNotFoundException;
import com.bioinformatics.dashboard.config.AppProperties;
import com.bioinformatics.dashboard.interfaces.UniProtApiClient;
import com.bioinformatics.dashboard.job.dto.Constants;
import com.bioinformatics.dashboard.job.dto.ImportJobProgress;
import com.bioinformatics.dashboard.job.dto.ImportJobSummary;
import com.bioinformatics.dashboard.job.dto.ImportStatus;
import com.bioinformatics.dashboard.job.entity.ImportJob;
import com.bioinformatics.dashboard.job.mapper.ImportJobMapper;
import com.bioinformatics.dashboard.job.repository.ImportJobRepository;
import com.bioinformatics.dashboard.job.uniprot.apiloader.UniProtApiImportJobExecutor;
import com.bioinformatics.dashboard.job.uniprot.fileloader.AsyncUniprotImportJobExecutor;
import com.bioinformatics.dashboard.job.uniprot.fileloader.counter.CounterRegistry;
import com.bioinformatics.dashboard.model.gene.PagedResponse;
import com.bioinformatics.dashboard.savedfilter.dto.SavedFilterDto;
import com.bioinformatics.dashboard.savedfilter.service.SavedFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import static com.bioinformatics.dashboard.job.dto.Constants.SAVED_FILTER_ID;

/**
 * Orchestrates the execution of asynchronous UniProt import jobs.
 * Enforces single-job concurrency and validates files before delegating to Spring Batch.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

    private final ImportJobRepository importJobRep;
    private final ImportJobMapper jobMapper;
    private final AsyncUniprotImportJobExecutor importExec;
    private final UniProtApiImportJobExecutor remoteImportExec;
    private final AppProperties appProperties;
    private final CounterRegistry registry;
    private final SavedFilterService savedFilterService;
    private final UniProtApiClient uniProtApiClient;

    public PagedResponse<ImportJobSummary> listImportJobs(int page, int size) {
        log.info("listImportJobs page: {}, size: {}", page, size);
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
            log.info("Triggering import for file {} with strategy {}", file.getOriginalFilename(), strategy);
            var target = saveImportFile(file, strategy);
            var savedJob = saveJob(target, strategy);
            log.info("Import job {} saved successfully, starting execution", savedJob.id());
            executeImport(savedJob, target);
            return savedJob;
        } catch (Exception e) {
            throw new ExecuteJobException("Failed to trigger import " + e.getMessage(), e);
        }
    }

    @Transactional
    public ImportJobSummary triggerRemoteImport(long filterId) {
        checkImportAlreadyRunning();
        try {
            var filter = savedFilterService.getSavedFilterById(filterId)
                    .orElseThrow(() -> new ResourceNotFoundException("Saved Filter with id %d not found".formatted(filterId)));
            log.info("Triggering remote UniProt API import for filter {}", filter.name());
            var savedJob = saveRemoteJob(filter);
            executeRemoteImport(savedJob, filterId);
            return savedJob;
        } catch (Exception e) {
            throw new ExecuteJobException("Failed to trigger remote import " + e.getMessage(), e);
        }
    }

    public ImportJobProgress getImportJobStatus(String jobId) {
        log.info("Getting import job status for job {}", jobId);
        var job = importJobRep
                .findById(UUID.fromString(jobId))
                .orElseThrow(() -> ResourceNotFoundException.forImportJob(jobId));
        return jobMapper.toJobProgress(job);
    }

    private void checkImportAlreadyRunning() {
        var running = importJobRep.findByStatus(ImportStatus.RUNNING);
        if (!running.isEmpty()) {
            throw new ImportAlreadyRunningException(running.getFirst().getId().toString());
        }

    }

    private Path saveImportFile(MultipartFile file, String strategy) throws IOException {
        var uploadDir = Paths.get(appProperties.getImportConfig().getTempDir());
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

    private ImportJobSummary saveRemoteJob(SavedFilterDto filter) {
        var totalRecords = countRemoteUniprotEntries(filter);
        var job = new ImportJob();
        job.setStatus(ImportStatus.RUNNING);
        job.setFileName("Filter <%s>".formatted(filter.name()));
        job.setStrategy("OVERWRITE");
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

    private int countRemoteUniprotEntries(SavedFilterDto filter) {
        try {
            var query = filter.filterJson().copy().size(1).page(0).build();
            var result = uniProtApiClient.fetchPage(query, null);
            return Math.toIntExact(result.totalElements());
        } catch (Exception e) {
            log.error("Failed to retrieve total count entries for saved filter {}", filter.name());
            return 0;
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

    private void executeRemoteImport(ImportJobSummary importJob, long filterId) {
        var parameters = new JobParametersBuilder()
                .addString(Constants.IMPORT_JOB_ID.getKey(), importJob.id())
                .addLong(Constants.TIMESTAMP.getKey(), System.currentTimeMillis())
                .addLong(SAVED_FILTER_ID.getKey(), filterId)
                .toJobParameters();
        remoteImportExec.execute(parameters);
    }


}

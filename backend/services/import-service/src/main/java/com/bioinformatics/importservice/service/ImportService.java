package com.bioinformatics.importservice.service;

import com.bioinformatics.common.exception.ConflictException;
import com.bioinformatics.common.exception.ExecuteJobException;
import com.bioinformatics.common.exception.MalformedFileException;
import com.bioinformatics.common.exception.ResourceNotFoundException;
import com.bioinformatics.common.models.PagedResponse;
import com.bioinformatics.common.models.filter.SavedFilterDto;
import com.bioinformatics.common.providers.DataProvider;
import com.bioinformatics.common.providers.uniprotkb.service.UniProtApiClient;
import com.bioinformatics.importservice.client.SavedFilterService;
import com.bioinformatics.importservice.config.ApplicationProperties;
import com.bioinformatics.importservice.dto.Constants;
import com.bioinformatics.importservice.dto.ImportJobProgress;
import com.bioinformatics.importservice.dto.ImportJobSummary;
import com.bioinformatics.importservice.dto.ImportStatus;
import com.bioinformatics.importservice.entity.ImportJob;
import com.bioinformatics.importservice.mapper.ImportJobMapper;
import com.bioinformatics.importservice.repository.ImportJobRepository;
import com.bioinformatics.importservice.uniprot.ImportJobExecutor;
import com.bioinformatics.importservice.uniprot.fileloader.counter.CounterRegistry;
import com.bioinformatics.shared.models.security.UserPrincipal;
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
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.bioinformatics.importservice.dto.Constants.*;


/**
 * Service for orchestrating asynchronous UniProt import jobs.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>File-based imports: upload, validate, count entries
 *   <li>API-based imports: fetch from saved filter, validate access
 *   <li>Enforce single-job concurrency control
 *   <li>Enqueue async batch jobs
 *   <li>Track import job status and progress
 * </ul>
 *
 * <p>Workflow:
 * {@code triggerImport/triggerRemoteImport} → {@code saveJob} → {@code executeImport}
 * → {@code ImportJobExecutor} → Batch Job → {@code ImportJobDatabaseListener}
 * → {@code ImportProgressChunkListener} → DB updates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

    private final ImportJobRepository importJobRep;
    private final ImportJobMapper jobMapper;
    private final ImportJobExecutor importJobExecutor;
    private final ApplicationProperties appProperties;
    private final CounterRegistry registry;
    private final SavedFilterService savedFilterService;
    private final UniProtApiClient uniProtApiClient;

    /**
     * Lists all import jobs with pagination.
     *
     * @param page zero-indexed page number
     * @param size page size
     * @return paginated list of import job summaries
     */
    public PagedResponse<ImportJobSummary> listImportJobs(int page, int size) {
        log.debug("[IMPORT] Listing import jobs - page={}, size={}", page, size);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var summaryPage = importJobRep.findAll(pageable).map(jobMapper::toSummary);
        log.debug("[IMPORT] Found {} import jobs", summaryPage.getTotalElements());

        return new PagedResponse<>(summaryPage.getContent(),
                summaryPage.getNumber(),
                summaryPage.getSize(),
                summaryPage.getTotalElements(),
                summaryPage.getTotalPages());
    }

    /**
     * Triggers a file-based import job.
     *
     * <p>Workflow:
     * <ol>
     *   <li>Check no other import is running (single-job concurrency)
     *   <li>Save uploaded file to temp directory
     *   <li>Count UniProt entries in file
     *   <li>Persist job record with status=RUNNING
     *   <li>Enqueue async batch job
     * </ol>
     *
     * @param file     uploaded file (.dat or .tsv)
     * @param strategy import strategy (OVERWRITE or APPEND)
     * @return job summary with ID and initial status
     * @throws ConflictException   if another import already running
     * @throws ExecuteJobException if job submission fails
     */
    @Transactional
    public ImportJobSummary triggerImport(MultipartFile file, String strategy) {
        log.info("[IMPORT] File import triggered - fileName='{}', strategy='{}', size={} bytes",
                file.getOriginalFilename(), strategy, file.getSize());

        checkImportAlreadyRunning();
        try {
            log.debug("[IMPORT] Saving file to temp directory - fileName='{}'", file.getOriginalFilename());
            var target = saveImportFile(file, strategy);
            log.debug("[IMPORT] File saved - path='{}', actualPath='{}'", file.getOriginalFilename(), target);

            log.debug("[IMPORT] Counting UniProt entries in file - path='{}'", target);
            var savedJob = saveJob(target, strategy);
            log.info("[IMPORT] Import job persisted to database - ID={}, status=RUNNING", savedJob.id());

            executeImport(savedJob, target);
            log.info("[IMPORT] Import job enqueued for async execution - ID={}", savedJob.id());
            return savedJob;
        } catch (Exception e) {
            log.error("[IMPORT] Failed to trigger file import - exception: {}", e.getMessage(), e);
            throw new ExecuteJobException("Failed to trigger import " + e.getMessage(), e);
        }
    }

    /**
     * Triggers a remote API-based import job using a saved filter.
     *
     * <p>Workflow:
     * <ol>
     *   <li>Check no other import is running
     *   <li>Fetch saved filter configuration from service
     *   <li>Query UniProt API to get total count
     *   <li>Persist job record with status=RUNNING
     *   <li>Enqueue async batch job with filter/user context
     * </ol>
     *
     * @param filterId  saved filter identifier
     * @param initiator authenticated user making the request
     * @return job summary with ID and initial status
     * @throws ResourceNotFoundException if filter not found or not accessible
     * @throws ConflictException if another import already running
     * @throws ExecuteJobException if job submission fails
     */
    @Transactional
    public ImportJobSummary triggerRemoteImport(long filterId, UserPrincipal initiator) {
        log.info("[IMPORT] Remote API import triggered - filterId={}, user='{}'", filterId, initiator.id());

        checkImportAlreadyRunning();
        try {
            log.debug("[IMPORT] Fetching saved filter - ID={}, user='{}'", filterId, initiator.id());
            var filter = savedFilterService.getSavedFilterById(filterId, initiator)
                    .orElseThrow(() -> {
                        log.warn("[IMPORT] Filter not found or not accessible - ID={}, user='{}'", filterId, initiator.id());
                        return new ResourceNotFoundException("Saved Filter with id %d not found".formatted(filterId));
                    });
            log.debug("[IMPORT] Filter retrieved - name='{}', ID={}", filter.name(), filterId);

            log.debug("[IMPORT] Counting entries from UniProt API - filter='{}'", filter.name());
            var savedJob = saveRemoteJob(filter);
            log.info("[IMPORT] Remote import job persisted to database - ID={}, status=RUNNING", savedJob.id());

            executeRemoteImport(savedJob, filterId, initiator);
            log.info("[IMPORT] Remote import job enqueued for async execution - ID={}", savedJob.id());
            return savedJob;
        } catch (Exception e) {
            log.error("[IMPORT] Failed to trigger remote import - exception: {}", e.getMessage(), e);
            throw new ExecuteJobException("Failed to trigger remote import " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the current status and progress of an import job.
     *
     * @param jobId import job UUID as string
     * @return job progress with status, counts, and timing
     * @throws ResourceNotFoundException if job not found
     */
    public ImportJobProgress getImportJobStatus(String jobId) {
        log.debug("[IMPORT] Fetching import job status - ID={}", jobId);
        var job = importJobRep
                .findById(UUID.fromString(jobId))
                .orElseThrow(() -> {
                    log.warn("[IMPORT] Import job not found - ID={}", jobId);
                    return ResourceNotFoundException.forImportJob(jobId);
                });
        log.debug("[IMPORT] Job status retrieved - ID={}, status={}, progress={}/{}",
                jobId, job.getStatus(), job.getRecordsProcessed(), job.getTotalEstimated());
        return jobMapper.toJobProgress(job);
    }

    /**
     * Validates that no other import job is currently running.
     *
     * @throws ConflictException if an import is already in progress
     */
    private void checkImportAlreadyRunning() {
        log.debug("[IMPORT] Checking for running imports");
        var running = importJobRep.findByStatus(ImportStatus.RUNNING);
        if (!running.isEmpty()) {
            var runningJobId = running.getFirst().getId().toString();
            log.warn("[IMPORT] Another import is already running - ID={}", runningJobId);
            throw new ConflictException("An import job is already running " + runningJobId);
        }
        log.debug("[IMPORT] No running imports found - safe to proceed");
    }

    /**
     * Saves an uploaded file to the temp directory.
     *
     * <p>Strategy logic:
     * <ul>
     *   <li>OVERWRITE: save with original filename, replace if exists
     *   <li>APPEND: save with UUID prefix to avoid collisions
     * </ul>
     *
     * @param file     uploaded file
     * @param strategy import strategy
     * @return file path
     * @throws IOException if file operation fails
     */
    private Path saveImportFile(MultipartFile file, String strategy) throws IOException {
        var uploadDir = Paths.get(appProperties.importConfig().tempDir());
        var fname = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        var target = uploadDir.resolve(fname);

        if ("overwrite".equalsIgnoreCase(strategy)) {
            log.debug("[IMPORT] Saving file with OVERWRITE strategy - fileName='{}'", fname);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } else {
            fname = UUID.randomUUID() + "_" + fname;
            target = uploadDir.resolve(fname);
            log.debug("[IMPORT] Saving file with APPEND strategy - newFileName='{}'", fname);
            Files.copy(file.getInputStream(), target);
        }
        log.debug("[IMPORT] File saved to disk - path='{}', size={} bytes", target, Files.size(target));
        return target;
    }

    /**
     * Creates and persists a new import job record.
     *
     * <p>Counts UniProt entries, creates entity, persists with status=RUNNING.
     *
     * @param file     file path
     * @param strategy import strategy
     * @return job summary
     */
    private ImportJobSummary saveJob(Path file, String strategy) {
        log.debug("[IMPORT] Counting UniProt entries in file - path='{}'", file);
        var totalRecords = countUniprotEntries(file);
        log.debug("[IMPORT] Entry count complete - path='{}', totalRecords={}", file, totalRecords);

        var job = new ImportJob();
        job.setStatus(ImportStatus.RUNNING);
        job.setFileName(file.getFileName().toString());
        job.setStrategy(strategy.toUpperCase());
        job.setTotalEstimated(totalRecords);

        var savedJob = importJobRep.save(job);
        log.debug("[IMPORT] Job record created and persisted - ID={}, fileName='{}', totalEstimated={}",
                savedJob.getId(), savedJob.getFileName(), totalRecords);
        return jobMapper.toSummary(savedJob);
    }

    /**
     * Creates and persists a new remote import job record.
     *
     * <p>Queries UniProt API for total count, creates entity, persists with status=RUNNING.
     *
     * @param filter saved filter with query criteria
     * @return job summary
     */
    private ImportJobSummary saveRemoteJob(SavedFilterDto filter) {
        log.debug("[IMPORT] Counting entries from UniProt API - filter='{}'", filter.name());
        var totalRecords = countRemoteUniprotEntries(filter);
        log.debug("[IMPORT] Remote count complete - filter='{}', totalRecords={}", filter.name(), totalRecords);

        var job = new ImportJob();
        job.setStatus(ImportStatus.RUNNING);
        job.setFileName("Filter <%s>".formatted(filter.name()));
        job.setStrategy("OVERWRITE");
        job.setTotalEstimated(totalRecords);

        var savedJob = importJobRep.save(job);
        log.debug("[IMPORT] Remote job record created and persisted - ID={}, fileName='{}', totalEstimated={}",
                savedJob.getId(), savedJob.getFileName(), totalRecords);
        return jobMapper.toSummary(savedJob);
    }

    /**
     * Counts UniProt entries in a file using the appropriate counter (DAT or TSV).
     *
     * @param file file path
     * @return number of entries
     * @throws MalformedFileException if file is invalid
     */
    private int countUniprotEntries(Path file) {
        try {
            var counter = registry.getCounter(file.toString());
            log.debug("[IMPORT] Using counter for file type - path='{}', counterType='{}'",
                    file, counter.getClass().getSimpleName());
            try (var is = Files.newInputStream(file)) {
                var count = (int) counter.count(is);
                log.debug("[IMPORT] File entry count complete - path='{}', count={}", file, count);
                return count;
            }
        } catch (Exception e) {
            log.error("[IMPORT] Failed to count UniProt entries - path='{}', exception: {}", file, e.getMessage(), e);
            throw new MalformedFileException(e.getMessage());
        }
    }

    /**
     * Queries UniProt API to get the total record count for a filter.
     *
     * @param filter saved filter with query criteria
     * @return total records or 0 if query fails
     */
    private int countRemoteUniprotEntries(SavedFilterDto filter) {
        try {
            log.debug("[IMPORT] Querying UniProt API for count - filter='{}'", filter.name());
            var query = filter.filterJson().copy().size(1).page(0).build();
            var result = uniProtApiClient.fetchPage(query, null);
            var totalElements = Math.toIntExact(result.totalElements());
            log.debug("[IMPORT] UniProt API query complete - filter='{}', totalElements={}", filter.name(), totalElements);
            return totalElements;
        } catch (Exception e) {
            log.error("[IMPORT] Failed to retrieve total count from UniProt API - filter='{}', exception: {}",
                    filter.name(), e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Enqueues a file-based import job for async batch processing.
     *
     * <p>Builds job parameters and submits to ImportJobExecutor.
     *
     * @param importJob job summary
     * @param file      file path
     */
    private void executeImport(final ImportJobSummary importJob, final Path file) {
        log.debug("[IMPORT] Building file import job parameters - ID={}, file='{}'", importJob.id(), file);
        var parameters = new JobParametersBuilder()
                .addString(Constants.IMPORT_JOB_ID.getKey(), importJob.id())
                .addString(Constants.FILE_PATH.getKey(), file.toAbsolutePath().toString())
                .addLong(Constants.TIMESTAMP.getKey(), System.currentTimeMillis())
                .addString(DATA_PROVIDER.getKey(), DataProvider.FILE.getKey())
                .toJobParameters();
        log.debug("[IMPORT] Submitting file import to async executor - ID={}", importJob.id());
        importJobExecutor.execute(parameters);
    }

    /**
     * Enqueues a remote API-based import job for async batch processing.
     *
     * <p>Builds job parameters with filter and user context and submits to ImportJobExecutor.
     *
     * @param importJob job summary
     * @param filterId  saved filter identifier
     * @param initiator user making the request
     */
    private void executeRemoteImport(ImportJobSummary importJob, long filterId, final UserPrincipal initiator) {
        log.debug("[IMPORT] Building remote import job parameters - ID={}, filterId={}, user='{}'",
                importJob.id(), filterId, initiator.id());

        var parameters = new JobParametersBuilder()
                .addString(Constants.IMPORT_JOB_ID.getKey(), importJob.id())
                .addLong(Constants.TIMESTAMP.getKey(), System.currentTimeMillis())
                .addString(DATA_PROVIDER.getKey(), DataProvider.API.getKey())
                .addLong(SAVED_FILTER_ID.getKey(), filterId);

        log.debug("[IMPORT] Adding user context to parameters - ID={}, user='{}', roles={}",
                importJob.id(), initiator.id(), initiator.roles());
        parameters.addString(USER_ID.getKey(), initiator.id());
        parameters.addJobParameter(USER_ROLE.getKey(), initiator.roles(), List.class);

        log.debug("[IMPORT] Submitting remote import to async executor - ID={}", importJob.id());
        importJobExecutor.execute(parameters.toJobParameters());
    }


}

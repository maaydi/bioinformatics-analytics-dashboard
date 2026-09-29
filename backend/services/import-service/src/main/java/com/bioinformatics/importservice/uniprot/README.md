# Unified UniProt Import Architecture

## Overview

This package implements a **unified Spring Batch job** that supports multiple UniProt data sources (API and file-based
imports) through a single, reusable orchestration layer. The job uses **runtime routing** via a `JobExecutionDecider` to
select the appropriate import step based on the `DATA_PROVIDER` job parameter.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     ImportJobConfig                         │
│              (Unified Job Orchestrator)                      │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  1. unifiedUniProtImportJob()                               │
│     ├─ Start: importSourceDecider()                         │
│     ├─ If DATA_PROVIDER = "api"  → uniProtApiImportStep    │
│     ├─ If DATA_PROVIDER = "file" → uniProtImportStep       │
│     └─ Post-process (listeners):                            │
│        ├─ ImportJobDatabaseListener                         │
│        ├─ PostImportCacheEvictionListener                   │
│        └─ ImportJobRefreshViewsListener                     │
│                                                               │
└─────────────────────────────────────────────────────────────┘
         ▲                                   ▲
         │                                   │
┌────────┴────────────────┐    ┌────────────┴──────────────┐
│                         │    │                           │
│  UniProtApiImportJobConfig   │  UniProtImportJobConfig   │
│  (API-based import step)     │  (File-based import step) │
│                         │    │                           │
│ ┌─────────────────┐    │    │  ┌─────────────────┐      │
│ │ UniProtApiItem  │    │    │  │ DynamicUniprot  │      │
│ │    Reader       │    │    │  │    Reader       │      │
│ └─────────────────┘    │    │  └─────────────────┘      │
│ ┌─────────────────┐    │    │  ┌─────────────────┐      │
│ │ UniProtApiEntry │    │    │  │ ProteinEntry    │      │
│ │  Processor      │    │    │  │  Processor      │      │
│ └─────────────────┘    │    │  └─────────────────┘      │
│         ▼                   │         ▼                   │
│  ProteinAggregate          │  ProteinAggregate          │
│    ItemWriter              │    ItemWriter              │
│                            │                            │
└────────────────────────────┴────────────────────────────┘
```

## Key Components

### 1. **ImportJobConfig** (`ImportJobConfig.java`)

The unified job orchestrator. Responsibilities:

- Define the job flow with routing logic
- Manage global listeners (database operations, cache eviction, view refresh)
- Inject source-specific steps (`uniProtApiImportStep`, `uniProtImportStep`)
- Create the `importSourceDecider()` bean

**Pattern:** Composition over inheritance. The job is agnostic to how each step is implemented.

### 2. **ImportSourceDecider** (`ImportSourceDecider.java`)

The decision point. Responsibilities:

- Read the `DATA_PROVIDER` job parameter
- Return flow status ("API", "FILE", or "UNKNOWN")
- Enable no-code routing — source selection is data-driven

**Implementation:** Spring Batch `JobExecutionDecider` interface.

### 3. **Step-Specific Configs**

- `apiloader/UniProtApiImportJobConfig.java` — defines `uniProtApiImportStep` (REST API → protein entries)
- `fileloader/UniProtImportJobConfig.java` — defines `uniProtImportStep` (.dat/.tsv file → protein entries)

Both steps conform to Spring Batch `Step` interface; the job is decoupled from their internals.

## How It Works

### Job Submission Flow

```
1. Caller (e.g., ImportService) creates JobParameters:
   - DATA_PROVIDER: "api" | "file"
   - SAVED_FILTER_ID: (for API), or FILE_PATH: (for file-based)
   - USER_ID, USER_ROLE, TIMESTAMP

2. JobLauncher.run(unifiedUniProtImportJob, jobParameters)

3. Job execution:
   a) Start importSourceDecider()
   b) Read DATA_PROVIDER parameter
   c) Return flow status ("API" or "FILE")
   d) Route to corresponding step
   e) Execute step (read → process → write)
   f) Apply post-step listeners

4. Job completion
   - SUCCESS: views refreshed, caches evicted, audit logged
   - FAILURE: listeners still execute for cleanup
```

### Example Usage

```java
// API-based import
var jobParams = new JobParametersBuilder()
                .addString("dataProvider", "api")
                .addLong("filterId", 42L)
                .addString("initiatorUserId", "alice@example.com")
                .addString("initiatorRole", "ADMIN")
                .toJobParameters();
jobLauncher.

run(unifiedUniProtImportJob, jobParams);

// File-based import
var jobParams = new JobParametersBuilder()
        .addString("dataProvider", "file")
        .addString("filePath", "/tmp/uniprot.dat")
        .addString("initiatorUserId", "bob@example.com")
        .addString("initiatorRole", "USER")
        .toJobParameters();
jobLauncher.

run(unifiedUniProtImportJob, jobParams);
```

## Design Principles

### 1. **Single Responsibility Principle (SRP)**

- Job orchestration (ImportJobConfig)
- Step implementation (API/file configs)
- Decision logic (ImportSourceDecider)
  → Each class has one reason to change.

### 2. **Open/Closed Principle (OCP)**

Adding a new import source (e.g., OMA, Ensembl) requires:

1. Create `OmaImportJobConfig` with step definition
2. Add decision path in `ImportJobConfig.unifiedUniProtImportJob()`
3. Add constants in `Constants` enum
   → No changes to existing classes.

### 3. **Liskov Substitution Principle (LSP)**

All steps are interchangeable `Step` implementations. The job doesn't know or care which step it runs; it only knows the
contract (Spring Batch `Step` interface).

### 4. **Strategy Pattern**

The step to execute is determined at runtime based on data. The job flow itself is invariant and reusable.

### 5. **Dependency Injection**

- `ImportJobConfig` injects `uniProtApiImportStep` and `uniProtImportStep` (not creating them)
- Listeners are injected, not hard-coded
  → Loose coupling, easy testing, configuration flexibility.

## Testing Strategy

### Unit Tests

- **ImportSourceDeciderTest:** Verify decider returns correct status for API, FILE, and null/unknown providers
- **ImportJobConfigTest:** Mock steps and verify job structure, listener execution order

### Integration Tests

- **ApiImportIntegrationTest:** Run full API → database flow with mocked API client
- **FileImportIntegrationTest:** Run full file → database flow with sample .dat/.tsv files
- **UnifiedImportIntegrationTest:** Verify both paths (API and file) produce identical database state

### Example Test Structure

```java

@SpringBatchTest
class UnifiedUniProtImportJobTest {
    @Test
    void apiImportStep_WritesProteinEntries_WhenDataProviderIsApi() {
        // Arrange
        var jobParams = new JobParametersBuilder()
                .addString("dataProvider", "api")
                .addLong("filterId", 42L)
                .toJobParameters();

        // Act
        var execution = jobLauncher.run(unifiedUniProtImportJob, jobParams);

        // Assert
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(proteinRepository.count()).isGreaterThan(0);
    }

    @Test
    void fileImportStep_WritesProteinEntries_WhenDataProviderIsFile() {
        // Similar test for file-based import
    }
}
```

## Configuration & Profiles

Both `ImportJobConfig` and step-specific configs are active only when `@Profile("!test")`. During testing, step
definitions must be mocked or a test-specific configuration must be provided.

### Development & Production

- No differences in job orchestration logic
- Differences are in external dependencies (API client mock vs. real, file paths, etc.)

## Future Extensions

### Adding a New Import Source

1. Create a new config package: `src/main/java/.../importservice/uniprot/omajobloader/`
2. Implement `OmaImportJobConfig` with step definition
3. Add constants: `OMA("oma")`, `OMA_IMPORT_STEP("omaImportStep")`
4. Update `ImportJobConfig.unifiedUniProtImportJob()`:
   ```java
   .from(importSourceDecider())
   .on(Constants.OMA.getKey()).to(omaImportStep)
   ```
5. Write integration tests

### Multi-Step Workflow

Current design supports a single conditional step. For more complex workflows (e.g., validation → import →
reconciliation):

- Extend the decider or add intermediate deciders
- Use flow builder `.next()` / `.on()` for sequential steps
- Keep orthogonal concerns (import vs. validation) in separate steps

### Error Handling & Retries

- Fault-tolerance is configured per-step (e.g., skip policies in file-based import)
- Job-level restart logic is handled by Spring Batch infrastructure
- Listeners can be extended to implement custom retry strategies

## Links & References

- **Spring Batch
  Documentation:** [JobExecutionDecider](https://docs.spring.io/spring-batch/reference/job.html#decidingFlows)
- **PIPE-001 Journal:** `documentation/implementation/PIPE-001/journal.md`
- **API Contract:** `documentation/api-contract.md` (if applicable to import endpoints)
- **Validation Rules:** `documentation/validation-rules.md` (protein data validation)


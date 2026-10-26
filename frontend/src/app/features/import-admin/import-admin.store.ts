import {HttpErrorResponse} from '@angular/common/http';
import {afterNextRender, computed, DestroyRef, effect, inject, Injectable, signal, untracked} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ImportJobCreated, ImportJobProgress, ImportJobSummary} from '@core/models/import.model';
import {SavedFilter} from '@core/models/saved-filter.model';
import {SavedFiltersService} from '@features/saved-filters/saved-filters.service';
import {
  catchError,
  defer,
  EMPTY,
  exhaustMap,
  expand,
  filter,
  finalize,
  interval,
  Observable,
  reduce,
  switchMap,
  take,
  takeWhile,
  tap,
} from 'rxjs';
import {IMPORT_ADMIN_MESSAGES as messages} from './import-admin.messages';
import {ImportAdminService} from './import-admin.service';

const POLL_INTERVAL_MS = 3000;
const SAVED_FILTER_PAGE_SIZE = 200;
const MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024 * 1024;
const ALLOWED_EXTENSIONS: readonly string[] = ['.dat', '.tsv'];

interface HistoryQuery {
  readonly page: number;
  readonly size: number;
  readonly revision: number;
}

@Injectable()
export class ImportAdminStore {
  private readonly api = inject(ImportAdminService);
  private readonly filtersApi = inject(SavedFiltersService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly ready = signal(false);
  private readonly remoteModeState = signal(false);
  private readonly selectedFilterIdState = signal<number | null>(null);
  private readonly selectedFileState = signal<File | null>(null);
  private readonly savedFiltersState = signal<readonly SavedFilter[]>([]);
  private readonly savedFiltersLoadingState = signal(false);
  private readonly savedFiltersErrorState = signal<string | null>(null);
  private readonly filtersRevision = signal(0);
  private readonly uploadingState = signal(false);
  private readonly progressState = signal(0);
  private readonly errorState = signal<string | null>(null);
  private readonly historyState = signal<readonly ImportJobSummary[]>([]);
  private readonly historyLoadingState = signal(false);
  private readonly totalState = signal(0);
  private readonly historyQuery = signal<HistoryQuery>({page: 0, size: 5, revision: 0});
  private readonly activeJobIdState = signal<string | null>(null);

  readonly isRemoteMode = this.remoteModeState.asReadonly();
  readonly selectedFilterId = this.selectedFilterIdState.asReadonly();
  readonly selectedFile = this.selectedFileState.asReadonly();
  readonly savedFilters = this.savedFiltersState.asReadonly();
  readonly savedFiltersLoading = this.savedFiltersLoadingState.asReadonly();
  readonly savedFiltersError = this.savedFiltersErrorState.asReadonly();
  readonly isUploading = this.uploadingState.asReadonly();
  readonly currentProgress = this.progressState.asReadonly();
  readonly errorMessage = this.errorState.asReadonly();
  readonly jobHistory = this.historyState.asReadonly();
  readonly historyLoading = this.historyLoadingState.asReadonly();
  readonly totalJobs = this.totalState.asReadonly();
  readonly pageIndex = computed(() => this.historyQuery().page);
  readonly pageSize = computed(() => this.historyQuery().size);
  readonly activeJobId = this.activeJobIdState.asReadonly();

  constructor() {
    afterNextRender(() => this.ready.set(true));

    effect(onCleanup => {
      this.filtersRevision();
      if (!this.isRemoteMode()) {
        this.savedFiltersState.set([]);
        this.savedFiltersLoadingState.set(false);
        return;
      }

      const subscription = untracked(() => {
        this.savedFiltersLoadingState.set(true);
        this.savedFiltersErrorState.set(null);
        return defer(() => this.filtersApi.listSavedFilters(0, SAVED_FILTER_PAGE_SIZE)).pipe(
          expand((response, index) => index + 1 < response.totalPages
            ? this.filtersApi.listSavedFilters(index + 1, SAVED_FILTER_PAGE_SIZE) : EMPTY, 1),
          reduce((filters, response) => [...filters, ...response.content], [] as readonly SavedFilter[]),
          catchError(() => {
            this.savedFiltersErrorState.set(messages.filtersLoadFailed);
            return EMPTY;
          }),
          finalize(() => this.savedFiltersLoadingState.set(false)),
        ).subscribe(filters => this.savedFiltersState.set(filters));
      });
      onCleanup(() => subscription.unsubscribe());
    });

    effect(onCleanup => {
      if (!this.ready()) return;
      const query = this.historyQuery();
      let initialLoad = true;
      this.historyLoadingState.set(true);
      // Poll only while history is unknown or work is active; never cancel a slow request on the next tick.
      const subscription = interval(POLL_INTERVAL_MS).pipe(
        filter(() => initialLoad || this.isUploading() || this.jobHistory().length === 0 ||
          this.jobHistory().some(job => job.status === 'RUNNING')),
        exhaustMap(() => {
          this.historyLoadingState.set(true);
          return defer(() => this.api.listImportJobs(query.page, query.size)).pipe(
            catchError(() => {
              this.errorState.set(messages.historyLoadFailed);
              return EMPTY;
            }),
            finalize(() => this.historyLoadingState.set(false)),
          );
        }),
      ).subscribe(history => {
        initialLoad = false;
        this.historyState.set(history.content);
        this.totalState.set(history.totalElements);
        const activeRow = history.content.find(job => job.id === this.activeJobId());
        if (activeRow) this.progressState.set(activeRow.progressPercent ?? 0);
      });
      onCleanup(() => {
        subscription.unsubscribe();
        this.historyLoadingState.set(false);
      });
    });
  }

  selectSource(isRemote: boolean): void {
    if (this.isUploading()) return;
    console.log(isRemote);
    this.remoteModeState.set(isRemote);
    this.errorState.set(null);
    this.selectedFilterIdState.set(null);
    this.selectedFileState.set(null);
    this.filtersRevision.update(revision => revision + 1);
  }

  selectFile(file: File): void {
    if (this.isUploading()) return;
    this.errorState.set(null);
    this.selectedFileState.set(null);
    if (file.size > MAX_FILE_SIZE_BYTES) {
      this.errorState.set(messages.fileTooLarge);
      return;
    }
    const extension = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
    if (!ALLOWED_EXTENSIONS.includes(extension)) {
      this.errorState.set(messages.unsupportedFile(ALLOWED_EXTENSIONS));
      return;
    }
    this.selectedFileState.set(file);
  }

  selectFilter(value: string): void {
    if (this.isUploading()) return;
    const id = value.trim() === '' ? NaN : Number(value);
    const selected = Number.isInteger(id) ? this.savedFilters().find(filter => filter.id === id) : undefined;
    this.selectedFilterIdState.set(selected?.id ?? null);
    if (selected && this.isRemoteMode()) this.submitRemoteImport();
  }

  submitImport(): void {
    if (this.isRemoteMode()) {
      this.submitRemoteImport();
      return;
    }
    const file = this.selectedFile();
    if (file) this.startImport(() => this.api.triggerImport(file));
  }

  submitRemoteImport(): void {
    const filterId = this.selectedFilterId();
    if (filterId !== null) this.startImport(() => this.api.triggerRemoteImport(filterId));
  }

  changePage(page: number, size: number): void {
    this.historyQuery.update(query => ({...query, page, size}));
  }

  private refreshHistory(): void {
    this.historyQuery.update(query => ({...query, revision: query.revision + 1}));
  }

  /** Submission and progress share a lifetime so leaving the feature cancels both. POSTs are never auto-retried. */
  private startImport(create: () => Observable<ImportJobCreated>): void {
    if (this.isUploading()) return;
    this.uploadingState.set(true);
    this.errorState.set(null);
    this.progressState.set(0);

    defer(create).pipe(
      take(1),
      switchMap(job => {
        if (!job?.id) {
          this.errorState.set(messages.missingJobId);
          return EMPTY;
        }
        this.activeJobIdState.set(job.id);
        return interval(POLL_INTERVAL_MS).pipe(
          exhaustMap(() => this.api.getJobProgress(job.id)),
          takeWhile(progress => progress.status === 'RUNNING', true),
          tap(progress => this.updateProgress(job.id, progress)),
          catchError(() => {
            this.errorState.set(messages.statusConnectionLost);
            return EMPTY;
          }),
        );
      }),
      catchError((error: unknown) => {
        this.errorState.set(this.importErrorMessage(error));
        return EMPTY;
      }),
      finalize(() => {
        this.uploadingState.set(false);
        this.activeJobIdState.set(null);
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  private updateProgress(jobId: string, progress: ImportJobProgress): void {
    const percent = progress.progressPercent || 0;
    this.progressState.set(percent);
    this.historyState.update(history => history.map(job => job.id === jobId
      ? {...job, progressPercent: percent, status: progress.status} : job));
    if (progress.status !== 'RUNNING') {
      this.selectedFileState.set(null);
      this.refreshHistory();
      if (progress.status === 'FAILED') this.errorState.set(messages.jobFailed(jobId));
    }
  }

  private importErrorMessage(error: unknown): string {
    switch (error instanceof HttpErrorResponse ? error.status : undefined) {
      case 409:
        return messages.conflict;
      case 413:
        return messages.payloadTooLarge;
      case 422:
        return messages.unsupportedType;
      default:
        return messages.unexpectedError;
    }
  }
}


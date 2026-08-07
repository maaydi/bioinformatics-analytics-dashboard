import {CommonModule, DecimalPipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';
import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  OnDestroy,
  signal,
  ViewChild,
} from '@angular/core';
import {MatButton} from '@angular/material/button';
import {
  MatCard,
  MatCardActions,
  MatCardContent,
  MatCardHeader,
  MatCardSubtitle,
  MatCardTitle,
} from '@angular/material/card';
import {MatIcon} from '@angular/material/icon';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatSlideToggle} from '@angular/material/slide-toggle';
import {MatTableModule} from '@angular/material/table';
import {ImportJobSummary} from '@core/models/import.model';
import {SavedFilter} from '@core/models/saved-filter.model';
import {SavedFiltersService} from '@features/saved-filters/saved-filters.service';
import {catchError, EMPTY, interval, Subscription, switchMap, takeWhile} from 'rxjs';
import {ImportAdminService} from './import-admin.service';
import {MatPaginator, PageEvent} from '@angular/material/paginator';

/**
 * Import Admin page — Epic 1 (US-1, US-2, US-3). ADMIN only.
 *
 * Features:
 * - File upload form (.dat / .tsv, max 2 GB)
 * - Local/remote source switch
 * - Saved-filter selection for remote imports
 * - Submit → POST /api/admin/import/uniprot → 202 Accepted
 * - Progress bar (polls GET /api/admin/import/status/{jobId} every 5 s)
 * - Import job history table (GET /api/admin/import/status)
 *
 * Error handling:
 * - 409 Conflict: "An import is already running"
 * - 413 Payload Too Large: file > 2 GB
 * - 422: unsupported file type
 *
 */

@Component({
  selector: 'app-import-admin',
  imports: [
    CommonModule,
    MatCard,
    MatCardHeader,
    MatCardTitle,
    MatCardSubtitle,
    MatCardContent,
    MatIcon,
    DecimalPipe,
    MatSlideToggle,
    MatProgressBarModule,
    MatCardActions,
    MatTableModule,
    MatButton,
    MatPaginator,
  ],
  templateUrl: './import-admin.component.html',
  styleUrl: './import-admin.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportAdminComponent implements OnDestroy {
  private readonly importService = inject(ImportAdminService);
  isRemoteMode = signal<boolean>(false);
  private readonly MAX_FILE_SIZE = 2 * 1024 * 1024 * 1024; // 2 GB
  private readonly ALLOWED_EXTENSIONS = ['.dat', '.tsv'];
  selectedFilterId = signal<number | null>(null);
  selectedFile = signal<File | null>(null);
  savedFilters = signal<SavedFilter[]>([]);
  savedFiltersLoading = signal<boolean>(false);
  savedFiltersError = signal<string | null>(null);
  displayedColumns: string[] = ['jobId', 'jobSource', 'status', 'progress', 'startTime', 'endTime'];
  isUploading = signal<boolean>(false);
  currentProgress = signal<number>(0);
  errorMessage = signal<string | null>(null);
  jobHistory = signal<ImportJobSummary[]>([]);
  totalJobs = signal<number>(0);
  pageSize = signal<number>(5);
  pageIndex = signal<number>(0);
  forceLoadHistory = signal<boolean>(false);
  private readonly savedFiltersService = inject(SavedFiltersService);

  private pollingSubscription?: Subscription;
  private historySubscription?: Subscription;
  private savedFiltersSubscription?: Subscription;

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor() {
    afterNextRender(() => {
      this.loadJobHistory();
    });
  }

  ngOnDestroy(): void {
    this.stopPolling();
    this.stopLoadHistory();
    this.stopLoadSavedFilters();
  }

  onSourceModeChange(isRemote: boolean): void {
    if (this.isUploading()) {
      return;
    }

    this.isRemoteMode.set(isRemote);
    this.errorMessage.set(null);
    this.selectedFilterId.set(null);
    this.clearSelectedFile();

    if (!isRemote) {
      this.stopLoadSavedFilters();
      this.savedFiltersLoading.set(false);
      this.savedFilters.set([]);
      return;
    }

    if (this.savedFilters().length === 0 && !this.savedFiltersLoading()) {
      this.loadSavedFilters();
    }
  }

  onFilterSelect(event: Event): void {
    if (this.isUploading()) {
      return;
    }

    const filterId = Number((event.target as HTMLSelectElement).value);
    const matchedFilter = Number.isInteger(filterId)
      ? this.savedFilters().find((filter) => filter.id === filterId)
      : undefined;
    const selectedFilterId = matchedFilter?.id ?? null;
    this.selectedFilterId.set(selectedFilterId);

    if (selectedFilterId !== null && this.isRemoteMode()) {
      this.submitRemoteImport();
    }
  }

  triggerFileInput(): void {
    this.fileInput.nativeElement.click();
  }

  onFileSelected(event: Event): void {
    if (this.isUploading()) {
      return;
    }

    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.errorMessage.set(null);

      if (file.size > this.MAX_FILE_SIZE) {
        this.errorMessage.set('Payload Too Large: file exceeds the 2 GB limit.');
        this.selectedFile.set(null);
        input.value = '';
        return;
      }
      const fileExt = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
      if (!this.ALLOWED_EXTENSIONS.includes(fileExt)) {
        this.errorMessage.set(
          `Unprocessable Entity: Unsupported file type. Only ${this.ALLOWED_EXTENSIONS} are allowed`,
        );
        this.selectedFile.set(null);
        input.value = '';
        return;
      }
      this.selectedFile.set(file);
    }
  }

  submitImport(): void {
    if (this.isUploading()) {
      return;
    }

    if (this.isRemoteMode()) {
      this.submitRemoteImport();
      return;
    }

    const file = this.selectedFile();
    if (!file) return;

    this.isUploading.set(true);
    this.errorMessage.set(null);
    this.currentProgress.set(0);

    this.importService.triggerImport(file).subscribe({
      next: (job) => {
        if (job?.id) {
          this.startPolling(job.id);
          return;
        }

        this.isUploading.set(false);
        this.errorMessage.set('The server did not return an import job identifier.');
      },
      error: (err: HttpErrorResponse) => {
        this.handleError(err);
        this.isUploading.set(false);
      },
    });
  }

  submitRemoteImport(): void {
    const filterId = this.selectedFilterId();
    if (filterId === null || this.isUploading()) return;

    this.isUploading.set(true);
    this.errorMessage.set(null);
    this.currentProgress.set(0);

    this.importService.triggerRemoteImport(filterId).subscribe({
      next: (job) => {
        if (job?.id) {
          this.startPolling(job.id);
          return;
        }

        this.isUploading.set(false);
        this.errorMessage.set('The server did not return an import job identifier.');
      },
      error: (err: HttpErrorResponse) => {
        this.handleError(err);
        this.isUploading.set(false);
      },
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.forceLoadHistory.set(true);
    this.loadJobHistory();
  }

  private handleError(error: HttpErrorResponse): void {
    let msg: string | null = null;
    switch (error.status) {
      case 409:
        msg = 'Conflict: An import is already running.';
        break;
      case 413:
        msg = 'Payload Too Large: The file size exceeds the 2 GB limit.';
        break;
      case 422:
        msg = 'Unprocessable Entity: Unsupported file type.';
        break;
      default:
        msg = 'An unexpected error occurred while communicating with server.';
        break;
    }
    this.errorMessage.set(msg);
  }

  private startPolling(jobId: string): void {
    this.stopPolling();
    this.pollingSubscription = interval(5000)
      .pipe(
        switchMap(() => this.importService.getJobProgress(jobId)),
        takeWhile(
          (jobProgress) => jobProgress.status !== 'COMPLETED' && jobProgress.status !== 'FAILED',
          true,
        ),
      )
      .subscribe({
        next: (jobProgress) => {
          this.currentProgress.set(jobProgress.progressPercent || 0);
          if (jobProgress.status === 'COMPLETED' || jobProgress.status === 'FAILED') {
            this.isUploading.set(false);
            this.selectedFile.set(null);
            this.loadJobHistory();
            if (jobProgress.status === 'FAILED') {
              this.errorMessage.set(`Import job ${jobId} failed to complete.`);
            }
          }
        },
        error: () => {
          this.errorMessage.set('Lost connection to server while checking import status');
          this.isUploading.set(false);
          this.stopPolling();
        },
      });
  }

  private stopPolling(): void {
    if (this.pollingSubscription) {
      this.pollingSubscription.unsubscribe();
      this.pollingSubscription = undefined;
    }
  }

  private loadJobHistory(): void {
    this.stopLoadHistory();
    this.historySubscription = interval(2000)
      .pipe(
        switchMap(() => {
          const hasRunning = this.jobHistory().some((job) => job.status === 'RUNNING');
          const hasHistory = this.jobHistory().length > 0;

          if (!this.isUploading() && !hasRunning && hasHistory && !this.forceLoadHistory()) {
            return EMPTY;
          }

          return this.importService.listImportJobs(this.pageIndex(), this.pageSize()).pipe(
            catchError(() => {
              this.errorMessage.set('Failed to load import job history.');
              return EMPTY;
            }),
          );
        }),
      )
      .subscribe({
        next: (history) => {
          this.jobHistory.set(history.content);
          this.totalJobs.set(history.totalElements);
          this.forceLoadHistory.set(false);
        },
        error: () => {
          this.errorMessage.set('Lost connection to server while loading job history');
          this.stopLoadHistory();
        },
      });
  }

  private stopLoadHistory(): void {
    if (this.historySubscription) {
      this.historySubscription.unsubscribe();
      this.historySubscription = undefined;
    }
  }

  private loadSavedFilters(page = 0, accumulatedFilters: SavedFilter[] = []): void {
    if (page === 0) {
      this.stopLoadSavedFilters();
      this.savedFiltersLoading.set(true);
      this.savedFiltersError.set(null);
    }

    const requestSubscription = this.savedFiltersService.listSavedFilters(page, 200).subscribe({
      next: (response) => {
        const allFilters = [...accumulatedFilters, ...response.content];
        if (response.totalPages <= page + 1) {
          this.savedFilters.set(allFilters);
          this.savedFiltersLoading.set(false);
          return;
        }

        this.loadSavedFilters(page + 1, allFilters);
      },
      error: () => {
        this.savedFiltersLoading.set(false);
        this.savedFiltersError.set('Failed to load saved filters.');
      },
    });

    if (!this.savedFiltersSubscription) {
      this.savedFiltersSubscription = new Subscription();
    }
    this.savedFiltersSubscription.add(requestSubscription);
  }

  private stopLoadSavedFilters(): void {
    if (this.savedFiltersSubscription) {
      this.savedFiltersSubscription.unsubscribe();
      this.savedFiltersSubscription = undefined;
    }
  }

  private clearSelectedFile(): void {
    this.selectedFile.set(null);
    if (this.fileInput?.nativeElement) {
      this.fileInput.nativeElement.value = '';
    }
  }
}

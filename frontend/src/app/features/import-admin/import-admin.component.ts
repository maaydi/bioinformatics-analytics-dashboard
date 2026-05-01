import { CommonModule, DecimalPipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  OnDestroy,
  OnInit,
  signal,
  ViewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatAnchor } from '@angular/material/button';
import {
  MatCard,
  MatCardActions,
  MatCardContent,
  MatCardHeader,
  MatCardSubtitle,
  MatCardTitle,
} from '@angular/material/card';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { interval, Subscription, switchMap, takeWhile } from 'rxjs';
/**
 * Import Admin page — Epic 1 (US-1, US-2, US-3). ADMIN only.
 *
 * Features:
 * - File upload form (.dat / .tsv, max 2 GB)
 * - Strategy selector (OVERWRITE)
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
interface JobStatus {
  jobId: string;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  progress: number;
  startTime?: string;
  endTime?: string;
}

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
    MatAnchor,
    DecimalPipe,
    MatFormField,
    MatLabel,
    MatSelectModule,
    MatProgressBarModule,
    MatCardActions,
    FormsModule,
    MatTableModule,
  ],
  templateUrl: './import-admin.component.html',
  styleUrl: './import-admin.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportAdminComponent implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly MAX_FILE_SIZE = 2 * 1024 * 1024 * 1024; // 2 GB
  private readonly ALLOWED_EXTENSIONS = ['.dat', '.tsv'];

  selectedFile = signal<File | null>(null);
  strategy = signal<string>('OVERWRITE');
  isUploading = signal<boolean>(false);
  currentProgress = signal<number>(0);
  errorMessage = signal<string | null>(null);
  jobHistory = signal<JobStatus[]>([]);

  displayedColumns: string[] = ['jobId', 'status', 'progress', 'startTime', 'endTime'];

  private pollingSubscription?: Subscription;

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  ngOnInit(): void {
    this.loadJobHistory();
  }
  ngOnDestroy(): void {
    this.stopPolling();
  }

  triggerFileInput() {
    this.fileInput.nativeElement.click();
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.errorMessage.set(null);

      if (file.size > this.MAX_FILE_SIZE) {
        this.errorMessage.set('Payload Too Large: file exceeds the 2 GN limit.');
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
    const file = this.selectedFile();
    if (!file) return;

    this.isUploading.set(true);
    this.errorMessage.set(null);
    this.currentProgress.set(0);

    const formData = new FormData();
    formData.append('file', file);
    formData.append('strategy', this.strategy());
    this.http
      .post<{ jobId: string }>('/api/admin/import/uniprot', FormData, {
        observe: 'response',
      })
      .subscribe({
        next: (response) => {
          if (response.status === 202 && response.body?.jobId) {
            this.startPolling(response.body.jobId);
          }
        },
        error: (err: HttpErrorResponse) => {
          this.handleError(err);
          this.isUploading.set(false);
        },
      });
  }

  private handleError(error: HttpErrorResponse) {
    let msg: string | null = null;
    switch (error.status) {
      case 409:
        msg = 'Conflict: An import is already running.';
        break;
      case 413:
        msg = 'Payload Too Large : The file side exceeds the 2 GB limit.';
        break;
      case 422:
        msg = 'Unprocessable Entity: Unsuported file type.';
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
        switchMap(() => this.http.get<JobStatus>(`/api/admin/import/status/${jobId}`)),
        takeWhile((status) => status.status !== 'COMPLETED' && status.status !== 'FAILED', true),
      )
      .subscribe({
        next: (status) => {
          this.currentProgress.set(status.progress || 0);
          if (status.status === 'COMPLETED' || status.status === 'FAILED') {
            this.isUploading.set(false);
            this.selectedFile.set(null);
            this.loadJobHistory();
            if (status.status === 'FAILED') {
              this.errorMessage.set(`import Job ${jobId} failed to complete.`);
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
  private stopPolling() {
    if (this.pollingSubscription) {
      this.pollingSubscription.unsubscribe();
      this.pollingSubscription = undefined;
    }
  }

  private loadJobHistory() {
    this.http.get<JobStatus[]>('/api/admin/import/status').subscribe({
      next: (history) => this.jobHistory.set(history),
      error: () => this.errorMessage.set('Failed to laod import job history.'),
    });
  }
}

import {CommonModule, DecimalPipe} from '@angular/common';
import {ChangeDetectionStrategy, Component, ElementRef, inject, viewChild,} from '@angular/core';
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
import {MatPaginator, PageEvent} from '@angular/material/paginator';
import {IMPORT_ADMIN_MESSAGES} from './import-admin.messages';
import {ImportAdminStore} from './import-admin.store';

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
  providers: [ImportAdminStore],
})
export class ImportAdminComponent {
  readonly displayedColumns: readonly string[] = ['jobId', 'jobSource', 'status', 'progress', 'startTime', 'endTime'];

  protected readonly messages = IMPORT_ADMIN_MESSAGES;
  private readonly fileInputElement = viewChild<ElementRef<HTMLInputElement>>('fileInput');

  private readonly store = inject(ImportAdminStore);
  readonly isRemoteMode = this.store.isRemoteMode;
  readonly selectedFilterId = this.store.selectedFilterId;
  readonly selectedFile = this.store.selectedFile;
  readonly savedFilters = this.store.savedFilters;
  readonly savedFiltersLoading = this.store.savedFiltersLoading;
  readonly savedFiltersError = this.store.savedFiltersError;
  readonly isUploading = this.store.isUploading;
  readonly currentProgress = this.store.currentProgress;
  readonly errorMessage = this.store.errorMessage;
  readonly jobHistory = this.store.jobHistory;
  readonly historyLoading = this.store.historyLoading;
  readonly totalJobs = this.store.totalJobs;
  readonly pageSize = this.store.pageSize;
  readonly pageIndex = this.store.pageIndex;

  onSourceModeChange(isRemote: boolean): void {
    if (this.isUploading()) return;
    const input = this.fileInputElement()?.nativeElement;
    if (input) input.value = '';
    this.store.selectSource(isRemote);
  }

  onFilterSelect(event: Event): void {
    const select = event.target;
    if (select instanceof HTMLSelectElement) this.store.selectFilter(select.value);
  }

  triggerFileInput(): void {
    if (!this.isUploading()) this.fileInputElement()?.nativeElement.click();
  }

  onFileSelected(event: Event): void {
    if (this.isUploading()) return;
    const input = event.target;
    if (!(input instanceof HTMLInputElement)) return;
    const file = input.files?.item(0);
    if (!file) return;
    this.store.selectFile(file);
    input.value = '';
  }

  submitImport(): void {
    this.store.submitImport();
  }

  onPageChange(event: PageEvent): void {
    this.store.changePage(event.pageIndex, event.pageSize);
  }
}

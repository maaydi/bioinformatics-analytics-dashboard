import {DatePipe, DecimalPipe} from '@angular/common';
import {ChangeDetectionStrategy, Component, DestroyRef, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatChipSelectionChange, MatChipsModule} from '@angular/material/chips';
import {MatDialog} from '@angular/material/dialog';
import {MatIcon} from '@angular/material/icon';
import {MatPaginator} from '@angular/material/paginator';
import {MatProgressBar} from '@angular/material/progress-bar';
import {MatTooltip} from '@angular/material/tooltip';
import {RouterLink} from '@angular/router';
import {ExportStatus} from '@core/models/export-pipeline.model';
import {ConfirmDialogComponent, DialogData} from '@shared/components/confirm-dialog/confirm-dialog.component';
import {EXPORT_PIPELINE_LIST_MESSAGES, EXPORT_STATUS_PRESENTATION} from './export-pipeline-list.messages';
import {ExportPipelineListStore, ExportPipelineRow} from './export-pipeline-list.store';

@Component({
  selector: 'app-export-pipeline-list',
  imports: [DatePipe, DecimalPipe, MatButtonModule, MatCardModule, MatChipsModule, MatIcon,
    MatPaginator, MatProgressBar, MatTooltip, RouterLink],
  providers: [ExportPipelineListStore],
  templateUrl: './export-pipeline-list.component.html',
  styleUrl: './export-pipeline-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExportPipelineListComponent {
  protected readonly store = inject(ExportPipelineListStore);
  protected readonly messages = EXPORT_PIPELINE_LIST_MESSAGES;
  protected readonly filters = [
    {value: undefined, label: this.messages.all},
    ...(['QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'] as const)
      .map(value => ({value, label: EXPORT_STATUS_PRESENTATION[value].label})),
  ] satisfies readonly {readonly value: ExportStatus | undefined; readonly label: string}[];
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  protected selectFilter(event: MatChipSelectionChange, status: ExportStatus | undefined): void {
    if (!event.isUserInput) return;
    if (event.selected) {
      this.store.selectStatus(status);
    } else if (this.store.status() === status) {
      // A single-selection filter always has an active option, including All.
      event.source.select();
    }
  }

  protected confirmDelete(row: ExportPipelineRow): void {
    if (row.busy) return;
    this.dialog.open<ConfirmDialogComponent, DialogData, boolean>(ConfirmDialogComponent, {
      width: '420px',
      maxWidth: '90vw',
      data: {
        title: this.messages.deleteTitle,
        message: this.messages.deletePrompt(row.name),
        confirmLabel: this.messages.confirmDelete,
        cancelLabel: this.messages.cancel,
      },
    }).afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(confirmed => {
      if (confirmed === true) this.store.delete(row);
    });
  }
}

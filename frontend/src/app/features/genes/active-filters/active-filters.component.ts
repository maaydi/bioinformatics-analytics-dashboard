import {ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, output} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {GeneSearchRequest} from '@core/models/saved-filter.model';
import {ExportPipeline} from '@core/models/export-pipeline.model';
import {MatIcon} from '@angular/material/icon';
import {buildFiltersChips, FilterChip} from '@shared/utils/filter-chips-builder';
import {MatButton} from '@angular/material/button';
import {MatDialog} from '@angular/material/dialog';
import {ExportPipelineWizardComponent} from '@features/export/export-pipeline-wizard/export-pipeline-wizard.component';
import {NotificationService} from '@shared/directive/notification.service';
import {ChipListComponent} from '@shared/components/chip-list/chip-list.component';
import {EXPORT_CONFIRMATION_MESSAGES} from '@features/export/export-pipeline-wizard/export-confirmation.messages';


@Component({
  selector: 'app-active-filters',
  imports: [
    MatIcon,
    MatButton,
    ChipListComponent
  ],
  templateUrl: './active-filters.component.html',
  styleUrl: './active-filters.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActiveFiltersComponent {
  readonly filterRemoved = output<keyof GeneSearchRequest>();
  readonly setChipsCount = output<number>();
  readonly filters = input<GeneSearchRequest | null>(null);
  readonly isExportinProgress = input<boolean>(false);
  readonly filtersChips = computed(() => this.buildChips(this.filters()));

  private readonly notify = inject(NotificationService);
  readonly readOnlyChips = input<boolean>(true);
  readonly title = input<string>('Active Filters');

  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);

  removeFilter(key: keyof GeneSearchRequest): void {
    this.filterRemoved.emit(key);
  }

  /** Converts non-empty filter fields into display chips. */
  private buildChips(filters: GeneSearchRequest | null): FilterChip[] {
    const chips = buildFiltersChips(filters);
    this.setChipsCount.emit(chips.length);
    return chips;
  }


  protected openCreateExportPipelineDialog(): void {
    const dialogRef = this.dialog.open<ExportPipelineWizardComponent, void, ExportPipeline | null>(ExportPipelineWizardComponent, {
      width: '800px',
      maxWidth: '90vw'
    });

    dialogRef.afterClosed().pipe(takeUntilDestroyed(this.destroyRef)).subscribe((response) => {
      if (!response) {
        return; // dialog dismissed without submitting
      }
      this.notify.success(EXPORT_CONFIRMATION_MESSAGES.created(response.name));
    });
  }
}

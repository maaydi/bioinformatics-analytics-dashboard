import {ChangeDetectionStrategy, Component, computed, inject, input, output} from '@angular/core';
import {GeneSearchRequest} from '@core/models/saved-filter.model';
import {MatChipSet} from '@angular/material/chips';
import {MatIcon} from '@angular/material/icon';
import {buildFiltersChips, FilterChip} from '@shared/utils/filter-chips-builder';
import {MatButton} from '@angular/material/button';
import {MatDialog} from '@angular/material/dialog';
import {ExportPipelineWizardComponent} from '@features/export/export-pipeline-wizard/export-pipeline-wizard.component';
import {NotificationService} from '@shared/directive/notification.service';


@Component({
  selector: 'app-active-filters',
  imports: [
    MatChipSet,
    MatIcon,
    MatButton
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

  constructor(private dialog: MatDialog) {
  }

  removeFilter(key: keyof GeneSearchRequest): void {
    this.filterRemoved.emit(key);
  }

  /** Converts non-empty filter fields into display chips. */
  private buildChips(filters: GeneSearchRequest | null): FilterChip[] {
    const chips = buildFiltersChips(filters);
    this.setChipsCount.emit(chips.length);
    return chips;
  }


  protected openCreateExportPipelineDialog() {
    const dialogRef = this.dialog.open(ExportPipelineWizardComponent, {
      width: '800px',
      maxWidth: '90vw'
    });

    dialogRef.afterClosed().subscribe((response: { name: string, success: boolean, error: string } | null) => {
      if (!response) {
        return; // dialog dismissed without submitting
      }
      if (response.success) {
        this.notify.success(`The export pipeline "${response.name}" was successfully created. You can track its progress in the Export section.`);
      } else {
        console.error(`Create Pipeline Fails : ${response.error}`);
        this.notify.error(`Failed to create the export pipeline "${response.name}". Please try again, or contact support if the issue persists.`);
      }
    });
  }
}

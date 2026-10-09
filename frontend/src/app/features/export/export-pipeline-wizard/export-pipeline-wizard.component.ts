import {ChangeDetectionStrategy, Component, computed, inject, viewChild} from '@angular/core';
import {MatDialogRef, MatDialogTitle} from '@angular/material/dialog';
import {MatStepper, MatStepperModule} from '@angular/material/stepper';
import {MatDivider} from '@angular/material/list';
import {MatButton} from '@angular/material/button';
import {GenesStore} from '@features/genes/state/filters.store';
import {ChipListComponent} from '@shared/components/chip-list/chip-list.component';
import {buildFiltersChips} from '@shared/utils/filter-chips-builder';

@Component({
  selector: 'app-export-pipeline-wizard',
  imports: [
    MatDialogTitle,
    MatStepperModule,
    MatDivider,
    MatButton,
    ChipListComponent
  ],
  templateUrl: './export-pipeline-wizard.component.html',
  styleUrl: './export-pipeline-wizard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExportPipelineWizardComponent {
  readonly store = inject(GenesStore);
  readonly filters = computed(() => this.store.activeFilters());
  readonly filtersChips = computed(() => buildFiltersChips(this.filters()));
  private readonly dialogRef = inject(MatDialogRef<ExportPipelineWizardComponent>);
  private readonly stepper = viewChild.required(MatStepper);

  protected next(): void {
    this.stepper().next();
  }

  protected previous(): void {
    this.stepper().previous();
  }

  protected cancel(): void {
    this.dialogRef.close(null);
  }
}

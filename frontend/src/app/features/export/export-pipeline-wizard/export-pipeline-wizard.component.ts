import {ChangeDetectionStrategy, Component, computed, inject, signal, viewChild} from '@angular/core';
import {MatDialogRef, MatDialogTitle} from '@angular/material/dialog';
import {MatStepper, MatStepperModule} from '@angular/material/stepper';
import {MatDivider} from '@angular/material/list';
import {MatButton} from '@angular/material/button';
import {GenesStore} from '@features/genes/state/filters.store';
import {ChipListComponent} from '@shared/components/chip-list/chip-list.component';
import {buildFiltersChips} from '@shared/utils/filter-chips-builder';
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {passwordMatchValidator} from '@shared/components/account-settings/account-settings.component';
import {ExportFieldsPickerComponent} from '@features/export/export-fields-picker/export-fields-picker.component';
import {ExportFieldSchema} from '@core/models/export-pipeline.model';
import {ExportPipelineService} from '@features/export/export-pipeline.service';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {catchError, of} from 'rxjs';
import {EXPORT_FIELD_SELECTION_MESSAGES} from '@features/export/export-field-selection.messages';

type FieldSelectionMode = 'ALL' | 'CUSTOM';

@Component({
  selector: 'app-export-pipeline-wizard',
  imports: [
    MatStepperModule,
    MatDialogTitle,
    MatDivider,
    MatButton,
    ChipListComponent,
    FormsModule,
    ReactiveFormsModule,
    ExportFieldsPickerComponent,
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
  readonly estimatedRows = computed<number>(() => this.store.searchResult()?.totalElements ?? 0);
  readonly availableFields = signal<ExportFieldSchema[]>([]);
  readonly form = new FormGroup(
    {
      fieldSchema: new FormControl<string[]>([], {
        nonNullable: true,
        validators: [Validators.required],
      }),
      name: new FormControl<string>('', {
        nonNullable: true,
        validators: [
          Validators.required,
        ],
      }),
      description: new FormControl<string>('', {
        nonNullable: true,
      }),
    },
    {validators: passwordMatchValidator},
  );
  protected readonly fieldSelectionMessages = EXPORT_FIELD_SELECTION_MESSAGES;
  protected readonly fieldMode = signal<FieldSelectionMode>('ALL');
  protected readonly fieldsLoadFailed = signal(false);
  private readonly exportPipelineService = inject(ExportPipelineService);
  private customFieldSelection: readonly string[] | undefined;

  constructor() {
    this.exportPipelineService.getAvailableFields().pipe(
      takeUntilDestroyed(),
      catchError(() => {
        this.fieldsLoadFailed.set(true);
        return of<ExportFieldSchema[]>([]);
      }),
    ).subscribe((fields) => {
      this.availableFields.set(fields);
      if (this.fieldMode() === 'ALL') {
        this.selectAllFields();
      }
    });
  }

  protected setFieldMode(mode: FieldSelectionMode): void {
    if (mode === this.fieldMode()) {
      return;
    }

    if (mode === 'ALL') {
      this.customFieldSelection = [...this.form.controls.fieldSchema.value];
      this.selectAllFields();
    } else if (this.customFieldSelection !== undefined) {
      this.form.controls.fieldSchema.setValue([...this.customFieldSelection]);
    }

    this.fieldMode.set(mode);
  }

  protected onSubmit() {

  }

  protected next(): void {
    this.stepper().next();
  }

  protected previous(): void {
    this.stepper().previous();
  }

  protected cancel(): void {
    this.dialogRef.close(null);
  }

  private selectAllFields(): void {
    this.form.controls.fieldSchema.setValue(this.availableFields().map((field) => field.fieldName));
  }
}

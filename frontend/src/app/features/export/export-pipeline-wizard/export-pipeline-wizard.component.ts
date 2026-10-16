import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  LOCALE_ID,
  signal,
  viewChild
} from '@angular/core';
import {DecimalPipe} from '@angular/common';
import {MatDialog, MatDialogRef, MatDialogTitle} from '@angular/material/dialog';
import {MatIcon} from '@angular/material/icon';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatStepper, MatStepperModule} from '@angular/material/stepper';
import {MatDivider} from '@angular/material/list';
import {MatButton} from '@angular/material/button';
import {GenesStore} from '@features/genes/state/filters.store';
import {ChipListComponent} from '@shared/components/chip-list/chip-list.component';
import {buildFiltersChips} from '@shared/utils/filter-chips-builder';
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {passwordMatchValidator} from '@shared/components/account-settings/account-settings.component';
import {ExportFieldsPickerComponent} from '@features/export/export-fields-picker/export-fields-picker.component';
import {ExportFieldSchema, ExportFormat} from '@core/models/export-pipeline.model';
import {ExportPipelineService} from '@features/export/export-pipeline.service';
import {takeUntilDestroyed, toSignal} from '@angular/core/rxjs-interop';
import {catchError, EMPTY, finalize, of, startWith, switchMap, tap} from 'rxjs';
import {EXPORT_FIELD_SELECTION_MESSAGES} from '@features/export/export-field-selection.messages';
import {estimateExportBytes, formatExportBytes} from '@features/export/export-size-estimator';
import {SavedFilter} from '@core/models/saved-filter.model';
import {SaveFiltersDialogComponent} from '@features/genes/save-filters-dialog/save-filters-dialog.component';
import {SavedFiltersService} from '@features/saved-filters/saved-filters.service';
import {EXPORT_FILTER_REVIEW_MESSAGES} from './export-filter-review.messages';

type FieldSelectionMode = 'ALL' | 'CUSTOM';

@Component({
  selector: 'app-export-pipeline-wizard',
  imports: [
    DecimalPipe,
    MatIcon,
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
      format: new FormControl<ExportFormat>('CSV', {
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
  protected readonly selectedFormat = toSignal(this.form.controls.format.valueChanges.pipe(
    startWith(this.form.controls.format.value),
  ), {requireSync: true});
  private readonly locale = inject(LOCALE_ID);
  private readonly selectedFields = toSignal(this.form.controls.fieldSchema.valueChanges.pipe(
    startWith(this.form.controls.fieldSchema.value),
  ), {requireSync: true});
  protected readonly estimatedSizes = computed<Readonly<Record<ExportFormat, string>>>(() => {
    const fields = this.selectedFields();
    const rows = this.estimatedRows();
    const estimate = (format: ExportFormat): string =>
      formatExportBytes(estimateExportBytes(fields, rows, format), this.locale);

    return {CSV: estimate('CSV'), TSV: estimate('TSV'), JSON: estimate('JSON'), EXCEL: estimate('EXCEL')};
  });
  protected readonly fieldSelectionMessages = EXPORT_FIELD_SELECTION_MESSAGES;
  protected readonly fieldMode = signal<FieldSelectionMode>('ALL');
  protected readonly fieldsLoadFailed = signal(false);
  private readonly exportPipelineService = inject(ExportPipelineService);
  private customFieldSelection: readonly string[] | undefined;
  protected readonly filterReviewMessages = EXPORT_FILTER_REVIEW_MESSAGES;
  protected readonly savingFilter = signal(false);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly savedFiltersService = inject(SavedFiltersService);
  private readonly destroyRef = inject(DestroyRef);

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

  /** Saves the reviewed snapshot without changing the active search or leaving the wizard. */
  protected saveFilter(): void {
    const filters = this.filters();
    if (filters === null || this.savingFilter()) {
      return;
    }

    // The shared dialog collects only a name; preserve the criteria reviewed when it opens.
    const filterJson = structuredClone(filters);
    this.savingFilter.set(true);
    this.dialog.open<SaveFiltersDialogComponent, void, Pick<SavedFilter, 'name'> | null>(
      SaveFiltersDialogComponent,
      {width: '420px'},
    ).afterClosed().pipe(
      switchMap((response) => {
        if (!response) {
          return EMPTY;
        }

        // SavedFiltersService retains the application's shared auth/refresh interceptor flow.
        return this.savedFiltersService.createSavedFilter({name: response.name, filterJson}).pipe(
          tap(() => this.snackBar.open(this.filterReviewMessages.savedFilter(response.name), this.filterReviewMessages.close, {
            duration: 4000,
            horizontalPosition: 'right',
            verticalPosition: 'top',
            panelClass: ['success-snackbar'],
          })),
          catchError(() => {
            this.snackBar.open(this.filterReviewMessages.saveFailed(response.name), this.filterReviewMessages.retry, {
              duration: 6000,
              horizontalPosition: 'right',
              verticalPosition: 'top',
              panelClass: ['error-snackbar'],
            });
            return EMPTY;
          }),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.savingFilter.set(false)),
    ).subscribe();
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

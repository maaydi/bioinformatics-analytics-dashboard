import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  LOCALE_ID,
  output,
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
import {ExportFieldsPickerComponent} from '@features/export/export-fields-picker/export-fields-picker.component';
import {
  ExportFieldSchema,
  ExportFormat,
  ExportPipeline,
  ExportPipelineCreateRequest
} from '@core/models/export-pipeline.model';
import {ExportPipelineService} from '@features/export/export-pipeline.service';
import {takeUntilDestroyed, toSignal} from '@angular/core/rxjs-interop';
import {catchError, defer, EMPTY, finalize, of, startWith, switchMap, take, tap} from 'rxjs';
import {EXPORT_FIELD_SELECTION_MESSAGES} from '@features/export/export-field-selection.messages';
import {estimateExportBytes, formatExportBytes} from '@features/export/export-size-estimator';
import {SavedFilter} from '@core/models/saved-filter.model';
import {SaveFiltersDialogComponent} from '@features/genes/save-filters-dialog/save-filters-dialog.component';
import {SavedFiltersService} from '@features/saved-filters/saved-filters.service';
import {EXPORT_FILTER_REVIEW_MESSAGES} from './export-filter-review.messages';
import {EXPORT_CONFIRMATION_MESSAGES} from './export-confirmation.messages';

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
  readonly pipelineCreated = output<ExportPipeline>();
  readonly store = inject(GenesStore);
  readonly filters = computed(() => this.store.activeFilters());
  readonly filtersChips = computed(() => buildFiltersChips(this.filters()));
  readonly form = new FormGroup(
    {
      fieldSchema: new FormControl<string[]>([], {
        nonNullable: true,
        validators: [Validators.required, Validators.maxLength(50)],
      }),
      format: new FormControl<ExportFormat>('CSV', {
        nonNullable: true,
        validators: [Validators.required],
      }),
      name: new FormControl<string>(`Export_${new Date().toISOString().slice(0, 10)}`, {
        nonNullable: true,
        validators: [
          Validators.required,
          Validators.pattern(/\S/),
          Validators.maxLength(200),
        ],
      }),
      description: new FormControl<string>('', {
        nonNullable: true,
        validators: [Validators.maxLength(500)],
      }),
    },
  );
  private readonly stepper = viewChild.required(MatStepper);
  readonly estimatedRows = computed<number>(() => this.store.searchResult()?.totalElements ?? 0);
  readonly availableFields = signal<ExportFieldSchema[]>([]);
  protected readonly confirmationMessages = EXPORT_CONFIRMATION_MESSAGES;
  protected readonly selectedStep = signal(0);
  protected readonly submitting = signal(false);
  protected readonly submitError = signal<string | null>(null);
  protected readonly fieldsLoading = signal(true);
  /** Count exactly the reviewed chips: arrays count once; false, zero and range bounds are retained. */
  protected readonly activeFilterCount = computed(() => this.filtersChips().length);
  protected readonly submissionBlockedMessage = computed(() => {
    const messages = this.confirmationMessages;
    if (this.fieldsLoadFailed()) return messages.fieldsUnavailable;
    if (this.fieldsLoading()) return messages.fieldsLoading;
    if (this.availableFields().length === 0) return messages.noFields;
    if (this.store.loading()) return messages.searchPending;
    if (this.store.onErrorMessage() || this.store.searchResult() === null) return messages.searchUnavailable;
    if (this.estimatedRows() <= 0) return messages.noRows;
    return null;
  });
  protected readonly selectedFields = toSignal(this.form.controls.fieldSchema.valueChanges.pipe(
    startWith(this.form.controls.fieldSchema.value),
  ), {requireSync: true});
  private readonly dialogRef = inject(MatDialogRef<ExportPipelineWizardComponent, ExportPipeline | null>);
  private readonly formEvents = toSignal(this.form.events);
  protected readonly submitDisabled = computed(() => {
    this.formEvents();
    return this.submitting() || this.selectedStep() !== 3 || this.form.invalid || this.form.pending ||
      this.submissionBlockedMessage() !== null;
  });
  protected readonly selectedFormat = toSignal(this.form.controls.format.valueChanges.pipe(
    startWith(this.form.controls.format.value),
  ), {requireSync: true});
  private readonly locale = inject(LOCALE_ID);
  private created = false;
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
      finalize(() => this.fieldsLoading.set(false)),
    ).subscribe((fields) => {
      this.fieldsLoading.set(false);
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

  protected showError<T>(control: FormControl<T>): boolean {
    this.formEvents();
    return control.invalid && (control.dirty || control.touched);
  }

  /** Submit only the reviewed final step; never automatically retry a potentially accepted POST. */
  protected onSubmit(): void {
    if (this.selectedStep() !== 3 || this.submitting() || this.created) {
      return;
    }
    this.form.markAllAsTouched();
    if (this.form.invalid || this.form.pending || this.submissionBlockedMessage() !== null) {
      return;
    }

    const {name, description, fieldSchema, format} = this.form.getRawValue();
    const trimmedDescription = description.trim();
    const request: ExportPipelineCreateRequest = {
      name: name.trim(),
      ...(trimmedDescription ? {description: trimmedDescription} : {}),
      // api-contract.md § POST /api/genes/search: all criteria are optional; {} means all data.
      // GenesStore keeps pagination separately. Do not send the UI's null sentinel to the API.
      filter: structuredClone(this.filters() ?? {}),
      format,
      fieldSchema: [...fieldSchema],
    };

    const previousDisableClose = this.dialogRef.disableClose;
    this.dialogRef.disableClose = true;
    this.submitError.set(null);
    this.submitting.set(true);
    defer(() => this.exportPipelineService.createPipeline(request)).pipe(
      take(1),
      tap((pipeline) => {
        sessionStorage.setItem('exportPipelineId', String(pipeline.id));
        this.created = true;
        this.pipelineCreated.emit(pipeline);
        this.dialogRef.close(pipeline);
      }),
      catchError(() => {
        this.submitError.set(this.confirmationMessages.createFailed);
        return EMPTY;
      }),
      takeUntilDestroyed(this.destroyRef),
      finalize(() => {
        this.dialogRef.disableClose = previousDisableClose;
        this.submitting.set(false);
      }),
    ).subscribe();
  }

  protected next(): void {
    if (!this.submitting()) this.stepper().next();
  }

  protected previous(): void {
    if (!this.submitting()) this.stepper().previous();
  }

  protected cancel(): void {
    if (!this.submitting() && !this.created) this.dialogRef.close(null);
  }

  private selectAllFields(): void {
    this.form.controls.fieldSchema.setValue(this.availableFields().map((field) => field.fieldName));
  }
}

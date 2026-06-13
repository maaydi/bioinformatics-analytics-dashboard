import {ChangeDetectionStrategy, ChangeDetectorRef, Component, effect, inject, input, output} from '@angular/core';
import {MatDialog, MatDialogModule} from '@angular/material/dialog';
import {MatSnackBar, MatSnackBarModule} from '@angular/material/snack-bar';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import {GeneFilterFormControls, GeneFilterFormValue, GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {MatCardContent, MatCardHeader} from '@angular/material/card';
import {MatIcon} from '@angular/material/icon';
import {MatButton} from '@angular/material/button';
import {MatDivider} from '@angular/material/divider';
import {MatInput} from '@angular/material/input';
import {MatError, MatFormField} from '@angular/material/form-field';
import {MatButtonToggle, MatButtonToggleGroup} from '@angular/material/button-toggle';
import {MatCheckbox} from '@angular/material/checkbox';
import {MatOption, MatSelect} from '@angular/material/select';
import {
  EVIDENCE_LEVEL_LABELS,
  EVIDENCE_LEVELS,
  EvidenceLevel,
  MAX_ACCESSION_LENGTH,
  MAX_GENE_NAME_PRIMARY_LENGTH,
  MAX_KEYWORD_LENGTH,
  MAX_KEYWORDS_COUNT,
  MAX_ORGANISM_LENGTH
} from '@core/models/protein.model';
import {InputComponent} from '@shared/components/input/input.component';
import {RangeInputComponent} from '@shared/components/range-input/range-input.component';
import {KeywordsFilterComponent} from '@features/genes/keywords-filter/keywords-filter.component';
import {SaveFiltersDialogComponent} from '@features/genes/save-filters-dialog/save-filters-dialog.component';
import {SavedFiltersService} from '@features/saved-filters/saved-filters.service';


const taxidPositiveIntegerValidator: ValidatorFn = (control: AbstractControl<number | null>): ValidationErrors | null => {
  const value = control.value;
  if (value === null) {
    return null;
  }
  if (!Number.isInteger(value) || value <= 0) {
    return {taxidPositiveInteger: true};
  }
  return null;
};

const keywordsValidator: ValidatorFn = (control: AbstractControl<string[] | null>): ValidationErrors | null => {
  const keywords = control.value ?? [];

  if (keywords.length > MAX_KEYWORDS_COUNT) {
    return {
      keywordsMaxCount: {
        max: MAX_KEYWORDS_COUNT,
        actual: keywords.length,
      }
    };
  }

  const oversizedKeyword = keywords.find((keyword) => keyword.length > MAX_KEYWORD_LENGTH);
  if (oversizedKeyword) {
    return {
      keywordMaxLength: {
        max: MAX_KEYWORD_LENGTH,
      }
    };
  }

  return null;
};
/**
 * Visual display mode for the filter panel.
 * - 'sidebar' = stacked vertical fields (default)
 * - 'grid' = compact multi-column layout (used by comparison view)
 */
export type DisplayMode = 'sidebar' | 'grid'

/**
 * Presentational filter panel for gene search criteria.
 *
 * Behavior:
 * - Maintains a reactive form for all supported gene filters.
 * - Prevents emission while the form is invalid.
 *
 * Outputs:
 * - `filterChange`: emits a normalized `GeneFilterSnapshot`.
 * - `filterClear`: emits after form reset.
 */
@Component({
  selector: 'app-gene-filter',
  imports: [ReactiveFormsModule,
    MatCardHeader,
    MatIcon,
    MatButton,
    MatDivider,
    MatCardContent,
    MatFormField,
    MatInput,
    MatButtonToggleGroup,
    MatButtonToggle,
    MatCheckbox,
    MatError,
    MatSelect,
    MatOption,
    InputComponent,
    RangeInputComponent,
    KeywordsFilterComponent,
    MatDialogModule,
    MatSnackBarModule],
  templateUrl: './gene-filter.component.html',
  styleUrls: ['./gene-filter.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GeneFilterComponent {

  displayMode = input<DisplayMode>('sidebar');
  /** Title shown in the header. Parent may override it */
  title = input<string>('Filters');
  readonly evidenceLevels = EVIDENCE_LEVELS;
  readonly form = new FormGroup<GeneFilterFormControls>({
    globalSearch: new FormControl('', {
      nonNullable: false
    }),
    accession: new FormControl('', {
      nonNullable: false,
      validators: [Validators.maxLength(MAX_ACCESSION_LENGTH)]
    }),
    entryName: new FormControl('', {nonNullable: false}),
    geneNamePrimary: new FormControl('', {
      nonNullable: false,
      validators: [Validators.maxLength(MAX_GENE_NAME_PRIMARY_LENGTH)]
    }),
    proteinFullName: new FormControl('', {nonNullable: false}),
    reviewed: new FormControl<boolean | null>(null),
    organism: new FormControl('', {
      nonNullable: false,
      validators: [Validators.maxLength(MAX_ORGANISM_LENGTH)]
    }),
    taxid: new FormControl<number | null>(null, {
      validators: [Validators.min(1), taxidPositiveIntegerValidator]
    }),
    lineage: new FormControl('', {nonNullable: false}),

    length: new FormControl<{ min: number | null; max: number | null }>({min: null, max: null}),
    molecularWeight: new FormControl<{ min: number | null; max: number | null }>({min: null, max: null}),

    evidenceLevels: new FormControl<EvidenceLevel[]>([]),
    keywords: new FormControl<string[]>([], {
      validators: [keywordsValidator],
    }),
    goTermId: new FormControl('', {
      nonNullable: false,
      validators: [Validators.pattern(/^GO:\d{7}$/)]
    }),
    goAspect: new FormControl<'P' | 'F' | 'C' | null>(null),
    featureType: new FormControl('', {nonNullable: false}),
    crossRefSource: new FormControl('', {nonNullable: false}),
  });
  readonly value = input<GeneFilterSnapshot | null>(null);

  readonly filterChange = output<GeneFilterSnapshot>();
  readonly filterClear = output<void>();
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  private readonly savedFiltersService = inject(SavedFiltersService);

  constructor() {
    effect(() => {
      const currentFormValues = this.form.getRawValue();
      const currentFilters = this.value();
      if (currentFilters) {
        const newFormValues = this.toForm(currentFilters);
        if (JSON.stringify(newFormValues) !== JSON.stringify(currentFormValues)) {
          this.form.patchValue(newFormValues, {emitEvent: false});
          this.cdr.markForCheck();
        }
      } else {
        const defaultValues = this.getDefaultFormValue();
        if (JSON.stringify(currentFilters) !== JSON.stringify(defaultValues)) {
          this.form.reset(this.getDefaultFormValue(), {emitEvent: false});
          this.cdr.markForCheck();
        }
      }
    });
  }

  /** Validates and emits the current filter snapshot. */
  applyFilters(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const snapshot = this.toSnapshot();
    this.filterChange.emit(snapshot);
  }

  /** Toggles one evidence level and emits the updated snapshot. */
  toggleEvidence(level: EvidenceLevel): void {
    const current = this.form.controls.evidenceLevels.value ?? [];
    const updated = current?.includes(level)
      ? current.filter((v) => v != level)
      : [...current, level];
    this.form.controls.evidenceLevels.setValue(updated);
    this.applyFilters();
  }

  /** Resets all filter fields to defaults and notifies the parent. */
  protected clearAll() {
    this.form.reset(this.getDefaultFormValue(), {emitEvent: false});
    this.filterClear.emit();
  }

  /** Opens the Save Filters dialog. The dialog handles submit/cancel logging. */
  protected openSaveFiltersDialog(): void {
    const dialogRef = this.dialog.open(SaveFiltersDialogComponent, {
      width: '420px'
    });

    dialogRef.afterClosed().subscribe((response: { name: string } | null) => {
      const filters = this.value();
      if (response && filters) {
        this.savedFiltersService.createSavedFilter({name: response.name, filterJson: filters})
          .subscribe({
            next: _saved => {
              this.snackBar.open(`Saved filter "${response.name}"`, 'Close', {
                duration: 4000,
                horizontalPosition: 'right',
                verticalPosition: 'top',
                panelClass: ['success-snackbar']
              });
            },
            error: _err => {
              this.snackBar.open(`Failed to save filter "${response?.name}"`, 'Retry', {
                duration: 6000,
                horizontalPosition: 'right',
                verticalPosition: 'top',
                panelClass: ['error-snackbar']
              });
            }
          });
      }
    });
  }

  protected isEvidenceSelected(level: EvidenceLevel): boolean | undefined {
    return this.form.controls.evidenceLevels.value?.includes(level);
  }

  /** Returns the display label for one evidence option. */
  protected getEvidenceLevel(level: EvidenceLevel): string {
    return `${level} - ${EVIDENCE_LEVEL_LABELS[level]}`;
  }

  /** Maps form raw values to the API-compatible filter snapshot. */
  private toSnapshot(): GeneFilterSnapshot {
    const rawValue = this.form.getRawValue();
    return {
      ...rawValue,
      globalSearch: rawValue.globalSearch && rawValue.globalSearch !== '' ? rawValue.globalSearch : null,
      lengthMin: rawValue.length ? rawValue.length.min : null,
      lengthMax: rawValue.length ? rawValue.length.max : null,
      molecularWeightMin: rawValue.molecularWeight ? rawValue.molecularWeight.min : null,
      molecularWeightMax: rawValue.molecularWeight ? rawValue.molecularWeight.max : null,
      goAspect: rawValue.goAspect ?? null,
      goTermId: rawValue.goTermId && rawValue.goTermId !== '' ? rawValue.goTermId : null
    };
  }

  private toForm(snapshot: GeneFilterSnapshot): Partial<GeneFilterFormValue> {
    return {
      globalSearch: snapshot.globalSearch ?? '',
      accession: snapshot.accession ?? '',
      entryName: snapshot.entryName ?? '',
      geneNamePrimary: snapshot.geneNamePrimary ?? '',
      proteinFullName: snapshot.proteinFullName ?? '',
      reviewed: snapshot.reviewed ?? null,
      organism: snapshot.organism ?? '',
      taxid: snapshot.taxid ?? null,
      lineage: snapshot.lineage ?? '',
      length: {min: snapshot.lengthMin ?? null, max: snapshot.lengthMax ?? null},
      molecularWeight: {min: snapshot.molecularWeightMin ?? null, max: snapshot.molecularWeightMax ?? null},
      evidenceLevels: snapshot.evidenceLevels ?? [],
      keywords: snapshot.keywords ?? [],
      goTermId: snapshot.goTermId ?? '',
      goAspect: snapshot.goAspect ?? null,
      featureType: snapshot.featureType ?? '',
      crossRefSource: snapshot.crossRefSource ?? '',
    };
  }

  private getDefaultFormValue(): GeneFilterFormValue {
    return {
      globalSearch: '',
      accession: '',
      entryName: '',
      geneNamePrimary: '',
      proteinFullName: '',
      reviewed: null,
      organism: '',
      taxid: null,
      lineage: '',
      length: {min: null, max: null},
      molecularWeight: {min: null, max: null},
      evidenceLevels: [],
      keywords: [],
      goTermId: '',
      goAspect: null,
      featureType: '',
      crossRefSource: '',
    };
  }


}

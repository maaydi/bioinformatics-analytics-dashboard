import {ChangeDetectionStrategy, Component, DestroyRef, effect, inject, input, output} from '@angular/core';
import {FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {GeneFilterFormControls, GeneFilterFormValue, GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {MatCardContent, MatCardHeader} from '@angular/material/card';
import {MatIcon} from '@angular/material/icon';
import {MatButton} from '@angular/material/button';
import {MatDivider} from '@angular/material/list';
import {debounceTime, distinctUntilChanged} from 'rxjs';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {MatError, MatFormField, MatInput} from '@angular/material/input';
import {MatButtonToggle, MatButtonToggleGroup} from '@angular/material/button-toggle';
import {MatCheckbox} from '@angular/material/checkbox';
import {MatOption, MatSelect} from '@angular/material/select';
import {EVIDENCE_LEVEL_LABELS, EVIDENCE_LEVELS, EvidenceLevel} from '@core/models/protein.model';
import {InputComponent} from '@shared/components/input/input.component';
import {RangeInputComponent} from '@shared/components/range-input/range-input.component';
import {KeywordsFilterComponent} from '@features/genes/keywords-filter/keywords-filter.component';


/**
 * Presentational filter panel for gene search criteria.
 *
 * Behavior:
 * - Maintains a reactive form for all supported gene filters.
 * - Debounce global search updates before emitting filters.
 * - Prevents emission while the form is invalid.
 *
 * Outputs:
 * - `filterChange`: emits a normalized `GeneFilterSnapshot`.
 * - `filterClear`: emits after form reset.
 */
@Component({
  selector: 'app-gene-filter',
  imports: [ReactiveFormsModule, MatCardHeader, MatIcon, MatButton, MatDivider, MatCardContent, MatFormField, MatInput, MatButtonToggleGroup, MatButtonToggle, MatCheckbox, MatError, MatSelect, MatOption, InputComponent, RangeInputComponent, KeywordsFilterComponent],
  templateUrl: './gene-filter.component.html',
  styleUrl: './gene-filter.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GeneFilterComponent {
  readonly evidenceLevels = EVIDENCE_LEVELS;
  private readonly fb = inject(FormBuilder);

  readonly value = input<GeneFilterSnapshot | null>(null);

  readonly filterChange = output<GeneFilterSnapshot>();
  readonly filterClear = output<void>();
  readonly form = new FormGroup<GeneFilterFormControls>({
    globalSearch: new FormControl('', {nonNullable: false}),
    accession: new FormControl('', {nonNullable: false}),
    entryName: new FormControl('', {nonNullable: false}),
    geneNamePrimary: new FormControl('', {nonNullable: false}),
    proteinFullName: new FormControl('', {nonNullable: false}),
    reviewed: new FormControl<boolean | null>(null),
    organism: new FormControl('', {nonNullable: false}),
    taxid: new FormControl<number | null>(null),
    lineage: new FormControl('', {nonNullable: false}),

    // Storing object values cleanly inside single form controls
    length: new FormControl<{ min: number | null; max: number | null }>({min: null, max: null}),
    molecularWeight: new FormControl<{ min: number | null; max: number | null }>({min: null, max: null}),

    evidenceLevels: new FormControl<EvidenceLevel[]>([]),
    keywords: new FormControl<string[]>([]),
    goTermId: new FormControl('', {
      nonNullable: false,
      validators: [Validators.pattern(/^GO:\d{7}$/)]
    }),
    goAspect: new FormControl<'P' | 'F' | 'C' | null>(null),
    featureType: new FormControl('', {nonNullable: false}),
    crossRefSource: new FormControl('', {nonNullable: false}),
  });
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.listenToGlobalSearch();
    effect(() => {
      const currentFilters = this.value();
      if (currentFilters) {
        this.form.patchValue(this.toForm(currentFilters), {emitEvent: false});
      } else {
        this.form.reset({}, {emitEvent: false});
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
    this.form.reset({
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
      goTermId: null,
      goAspect: null,
      featureType: '',
      crossRefSource: '',
    }, {emitEvent: false});
    this.filterClear.emit();
  }

  protected isEvidenceSelected(level: EvidenceLevel): boolean | undefined {
    return this.form.controls.evidenceLevels.value?.includes(level);
  }

  /** Returns the display label for one evidence option. */
  protected getEvidenceLevel(level: EvidenceLevel): String {
    return `${level} - ${EVIDENCE_LEVEL_LABELS[level]}`;
  }

  /** Subscribes to global-search changes with debounce and distinct guards. */
  private listenToGlobalSearch(): void {
    this.form.controls.globalSearch.valueChanges
      .pipe(debounceTime(400), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.form.pristine && !this.form.controls.globalSearch.value) {
          return;
        }
        this.applyFilters();
      });
  }

  /** Maps form raw values to the API-compatible filter snapshot. */
  private toSnapshot(): GeneFilterSnapshot {
    const rawValue = this.form.getRawValue();
    return {
      ...rawValue,
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
      reviewed: snapshot.reviewed,
      organism: snapshot.organism ?? '',
      taxid: snapshot.taxid,
      lineage: snapshot.lineage ?? '',
      length: {min: snapshot.lengthMin ?? null, max: snapshot.lengthMax ?? null},
      molecularWeight: {min: snapshot.molecularWeightMin ?? null, max: snapshot.molecularWeightMax ?? null},
      evidenceLevels: snapshot.evidenceLevels ?? [],
      keywords: snapshot.keywords ?? [],
      goTermId: snapshot.goTermId ?? '',
      goAspect: snapshot.goAspect,
      featureType: snapshot.featureType ?? '',
      crossRefSource: snapshot.crossRefSource ?? '',
    };
  }


}

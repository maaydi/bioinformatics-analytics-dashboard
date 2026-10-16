import {ComponentFixture, TestBed} from '@angular/core/testing';
import {signal} from '@angular/core';
import {HttpErrorResponse} from '@angular/common/http';
import {By} from '@angular/platform-browser';
import {MatDialog, MatDialogRef} from '@angular/material/dialog';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatStepper} from '@angular/material/stepper';
import {Router} from '@angular/router';
import {Validators} from '@angular/forms';
import {of, Subject} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ExportFieldSchema, ExportFormat} from '@core/models/export-pipeline.model';
import {GenesStore} from '@features/genes/state/filters.store';
import {ExportPipelineService} from '@features/export/export-pipeline.service';
import {GeneSearchRequest, SavedFilter} from '@core/models/saved-filter.model';
import {PagedResponse} from '@core/models/paged-response.model';
import {ProteinSummary} from '@core/models/protein.model';
import {SaveFiltersDialogComponent} from '@features/genes/save-filters-dialog/save-filters-dialog.component';
import {SavedFiltersService} from '@features/saved-filters/saved-filters.service';
import {ExportPipelineWizardComponent} from './export-pipeline-wizard.component';

const fields: ExportFieldSchema[] = [
  {fieldName: 'accession', displayName: 'Accession', dataType: 'STRING', description: 'Stable identifier.'},
  {fieldName: 'length', displayName: 'Sequence length', dataType: 'NUMBER', description: 'Number of residues.'},
];

describe('ExportPipelineWizardComponent', () => {
  let component: ExportPipelineWizardComponent;
  let fixture: ComponentFixture<ExportPipelineWizardComponent>;
  let availableFields$: Subject<ExportFieldSchema[]>;
  const getAvailableFields = vi.fn<ExportPipelineService['getAvailableFields']>();
  const activeFilters = signal<GeneSearchRequest | null>(null);
  const searchResult = signal<PagedResponse<ProteinSummary> | null>(null);
  const openDialog = vi.fn();
  const closeWizard = vi.fn();
  const openSnackBar = vi.fn<MatSnackBar['open']>();
  const createSavedFilter = vi.fn<SavedFiltersService['createSavedFilter']>();
  const navigate = vi.fn<Router['navigate']>();
  const navigateByUrl = vi.fn<Router['navigateByUrl']>();
  let dialogResult$: Subject<Pick<SavedFilter, 'name'> | null | undefined>;
  let savedFilter$: Subject<SavedFilter>;

  const currentFilters: GeneSearchRequest = {
    globalSearch: 'kinase', reviewed: false, taxid: 9606, lengthMin: 0,
    lengthMax: 500, evidenceLevels: [1, 2], keywords: ['ATP-binding'],
  };
  const savedFilter: SavedFilter = {
    id: 7, name: 'Kinases', filterJson: currentFilters, createdAt: '2026-01-01T00:00:00Z',
  };

  function element<T extends Element>(selector: string): T {
    const root = fixture.nativeElement as HTMLElement;
    const result = root.querySelector<T>(selector);
    if (result === null) {
      throw new Error(`Expected element: ${selector}`);
    }
    return result;
  }

  async function showStep(index: number): Promise<void> {
    await fixture.whenStable();
    fixture.debugElement.query(By.directive(MatStepper)).injector.get(MatStepper).selectedIndex = index;
    await fixture.whenStable();
  }

  function expectCardSizes(expected: Readonly<Record<ExportFormat, string>>): void {
    for (const format of ['CSV', 'TSV', 'JSON', 'EXCEL'] as const) {
      expect(element<HTMLElement>(`input[value="${format}"] + .format-card .format-size`).textContent?.trim())
        .toBe(`~ ${expected[format]}`);
    }
  }

  function summaryValue(label: string): string | undefined {
    const rows = element<HTMLElement>('.summary-card').querySelectorAll('.summary-row');
    return Array.from(rows).find((row) => row.querySelector('dt')?.textContent === label)
      ?.querySelector('dd')?.textContent?.trim();
  }

  async function selectMode(mode: 'ALL' | 'CUSTOM'): Promise<void> {
    element<HTMLInputElement>(`input[name="fieldMode"][value="${mode}"]`).click();
    await fixture.whenStable();
  }

  async function reviewFilters(): Promise<void> {
    element<HTMLButtonElement>('.wizard-nav-steps button:first-child').click();
    await fixture.whenStable();
  }

  async function openSaveFilter(): Promise<void> {
    await reviewFilters();
    element<HTMLButtonElement>('.save-filter-action').click();
    await fixture.whenStable();
  }

  async function closeSaveDialog(result: Pick<SavedFilter, 'name'> | null | undefined): Promise<void> {
    dialogResult$.next(result);
    dialogResult$.complete();
    await fixture.whenStable();
  }

  beforeEach(async () => {
    availableFields$ = new Subject<ExportFieldSchema[]>();
    getAvailableFields.mockReset().mockReturnValue(availableFields$.asObservable());
    activeFilters.set(null);
    searchResult.set(null);
    dialogResult$ = new Subject<Pick<SavedFilter, 'name'> | null | undefined>();
    savedFilter$ = new Subject<SavedFilter>();
    openDialog.mockReset().mockReturnValue({afterClosed: () => dialogResult$.asObservable()});
    createSavedFilter.mockReset().mockReturnValue(savedFilter$.asObservable());
    closeWizard.mockReset();
    openSnackBar.mockReset();
    navigate.mockReset();
    navigateByUrl.mockReset();

    await TestBed.configureTestingModule({
      imports: [ExportPipelineWizardComponent],
      providers: [
        {
          provide: GenesStore,
          useValue: {activeFilters, searchResult},
        },
        {provide: MatDialogRef, useValue: {close: closeWizard}},
        {provide: MatDialog, useValue: {open: openDialog}},
        {provide: MatSnackBar, useValue: {open: openSnackBar}},
        {provide: SavedFiltersService, useValue: {createSavedFilter}},
        {provide: Router, useValue: {navigate, navigateByUrl}},
        {provide: ExportPipelineService, useValue: {getAvailableFields}},
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ExportPipelineWizardComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
    element<HTMLButtonElement>('.wizard-nav-steps button:last-child').click();
    await fixture.whenStable();
  });

  it('defaults to ALL, showing the exact hint instead of the picker', () => {
    expect(element<HTMLInputElement>('input[name="fieldMode"][value="ALL"]').checked).toBe(true);
    expect(element<HTMLElement>('#step-fields-title + .toggle-group + .step-hint').textContent?.trim())
      .toBe('All available fields will be included in the export.');
    expect((fixture.nativeElement as HTMLElement).querySelector('app-export-fields-picker')).toBeNull();
    expect(component.form.controls.fieldSchema.value).toEqual([]);
  });

  it('defaults to a required non-nullable CSV control and zero size hints before fields arrive', async () => {
    await showStep(2);
    expect(component.form.controls.format.value).toBe('CSV');
    expect(component.form.controls.format.defaultValue).toBe('CSV');
    expect(component.form.controls.format.hasValidator(Validators.required)).toBe(true);
    expect(element<HTMLInputElement>('input[name="format"][value="CSV"]').checked).toBe(true);
    expectCardSizes({CSV: '0 B', TSV: '0 B', JSON: '0 B', EXCEL: '0 B'});

    component.form.controls.format.setValue('JSON');
    component.form.controls.format.reset();
    await fixture.whenStable();
    expect(component.form.controls.format.value).toBe('CSV');
    expect(element<HTMLInputElement>('input[name="format"][value="CSV"]').checked).toBe(true);
  });

  it.each([
    ['CSV', '24 KB'], ['TSV', '20 KB'], ['JSON', '40 KB'], ['EXCEL', '32 KB'],
  ] as const)('selecting the %s radio updates the form and selected summary', async (format, size) => {
    searchResult.set({content: [], page: 0, size: 50, totalElements: 1000, totalPages: 20});
    availableFields$.next(fields);
    await showStep(2);
    // Start away from the default to also exercise selecting CSV rather than only its initial state.
    component.form.controls.format.setValue(format === 'JSON' ? 'TSV' : 'JSON');
    await fixture.whenStable();
    element<HTMLInputElement>(`input[name="format"][value="${format}"]`).click();
    await fixture.whenStable();

    expect(component.form.controls.format.value).toBe(format);
    for (const candidate of ['CSV', 'TSV', 'JSON', 'EXCEL'] as const) {
      expect(element<HTMLInputElement>(`input[name="format"][value="${candidate}"]`).checked).toBe(candidate === format);
    }
    expectCardSizes({CSV: '24 KB', TSV: '20 KB', JSON: '40 KB', EXCEL: '32 KB'});
    await showStep(3);
    expect(summaryValue('Format')).toBe(format);
    expect(summaryValue('Estimated size')).toBe(`~ ${size}`);
    expect(getAvailableFields).toHaveBeenCalledOnce();
  });

  it('recalculates every card and the selected summary when fields, row count and format change', async () => {
    searchResult.set({content: [], page: 0, size: 50, totalElements: 1000, totalPages: 20});
    await showStep(2);
    expectCardSizes({CSV: '0 B', TSV: '0 B', JSON: '0 B', EXCEL: '0 B'});
    availableFields$.next(fields);
    await fixture.whenStable();
    expectCardSizes({CSV: '24 KB', TSV: '20 KB', JSON: '40 KB', EXCEL: '32 KB'});

    component.form.controls.fieldSchema.setValue(['geneNamePrimary']);
    await fixture.whenStable();
    expectCardSizes({CSV: '15 KB', TSV: '12.5 KB', JSON: '25 KB', EXCEL: '20 KB'});

    searchResult.set({content: [], page: 0, size: 50, totalElements: 2000, totalPages: 40});
    await fixture.whenStable();
    expectCardSizes({CSV: '30 KB', TSV: '25 KB', JSON: '50 KB', EXCEL: '40 KB'});
    await showStep(3);
    expect(summaryValue('Format')).toBe('CSV');
    expect(summaryValue('Estimated size')).toBe('~ 30 KB');

    component.form.controls.format.setValue('JSON');
    await fixture.whenStable();
    expect(summaryValue('Format')).toBe('JSON');
    expect(summaryValue('Estimated size')).toBe('~ 50 KB');
    await showStep(2);
    expect(element<HTMLInputElement>('input[name="format"][value="JSON"]').checked).toBe(true);
    expectCardSizes({CSV: '30 KB', TSV: '25 KB', JSON: '50 KB', EXCEL: '40 KB'});

    await showStep(3);
    component.form.controls.fieldSchema.setValue(['accession', 'length']);
    await fixture.whenStable();
    expect(summaryValue('Estimated size')).toBe('~ 80 KB');
    searchResult.set(null);
    await fixture.whenStable();
    expect(summaryValue('Estimated size')).toBe('~ 0 B');
    await showStep(2);
    expectCardSizes({CSV: '0 B', TSV: '0 B', JSON: '0 B', EXCEL: '0 B'});

    searchResult.set({content: [], page: 0, size: 50, totalElements: 1000, totalPages: 20});
    component.form.controls.fieldSchema.setValue([]);
    await fixture.whenStable();
    expectCardSizes({CSV: '0 B', TSV: '0 B', JSON: '0 B', EXCEL: '0 B'});
    await showStep(3);
    expect(summaryValue('Estimated size')).toBe('~ 0 B');
    expect(getAvailableFields).toHaveBeenCalledOnce();
  });

  it('keeps estimates aligned with picker edits and restored ALL/CUSTOM selections', async () => {
    searchResult.set({content: [], page: 0, size: 50, totalElements: 1000, totalPages: 20});
    availableFields$.next(fields);
    await selectMode('CUSTOM');
    element<HTMLInputElement>('app-export-fields-picker input[type="checkbox"]').click();
    await showStep(2);
    expectCardSizes({CSV: '9 KB', TSV: '7.5 KB', JSON: '15 KB', EXCEL: '12 KB'});

    await showStep(1);
    await selectMode('ALL');
    await showStep(2);
    expectCardSizes({CSV: '24 KB', TSV: '20 KB', JSON: '40 KB', EXCEL: '32 KB'});
    await showStep(1);
    await selectMode('CUSTOM');
    await showStep(2);
    expectCardSizes({CSV: '9 KB', TSV: '7.5 KB', JSON: '15 KB', EXCEL: '12 KB'});

    await showStep(1);
    element<HTMLButtonElement>('app-export-fields-picker button:last-child').click();
    await selectMode('ALL');
    await selectMode('CUSTOM');
    await showStep(2);
    expectCardSizes({CSV: '0 B', TSV: '0 B', JSON: '0 B', EXCEL: '0 B'});
    await showStep(3);
    expect(summaryValue('Estimated size')).toBe('~ 0 B');
  });

  it('cancels without submitting and starts a new wizard with CSV rather than the prior format', async () => {
    await showStep(2);
    element<HTMLInputElement>('input[name="format"][value="EXCEL"]').click();
    await fixture.whenStable();
    expect(component.form.controls.format.value).toBe('EXCEL');
    element<HTMLButtonElement>('.wizard-nav > button').click();
    expect(closeWizard).toHaveBeenCalledExactlyOnceWith(null);
    expect(createSavedFilter).not.toHaveBeenCalled();
    fixture.destroy();
    expect(availableFields$.observed).toBe(false);

    fixture = TestBed.createComponent(ExportPipelineWizardComponent);
    component = fixture.componentInstance;
    await showStep(2);
    expect(component.form.controls.format.value).toBe('CSV');
    expect(element<HTMLInputElement>('input[name="format"][value="CSV"]').checked).toBe(true);
    expectCardSizes({CSV: '0 B', TSV: '0 B', JSON: '0 B', EXCEL: '0 B'});
  });

  it('synchronizes ALL with asynchronously loaded fields even when the control already has a subset', async () => {
    component.form.controls.fieldSchema.setValue(['accession']);
    availableFields$.next(fields);
    await fixture.whenStable();

    expect(component.form.controls.fieldSchema.value).toEqual(['accession', 'length']);
    expect(component.form.controls.fieldSchema.valid).toBe(true);

    availableFields$.next([...fields, {
      fieldName: 'reviewed', displayName: 'Reviewed', dataType: 'BOOLEAN', description: '',
    }]);
    expect(component.form.controls.fieldSchema.value).toEqual(['accession', 'length', 'reviewed']);

    availableFields$.next([]);
    expect(component.form.controls.fieldSchema.value).toEqual([]);
    expect(component.form.controls.fieldSchema.hasError('required')).toBe(true);
  });

  it('initializes ALL for a synchronous field response too', async () => {
    fixture.destroy();
    searchResult.set({content: [], page: 0, size: 50, totalElements: 1000, totalPages: 20});
    getAvailableFields.mockReturnValue(of(fields));
    fixture = TestBed.createComponent(ExportPipelineWizardComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();

    expect(component.form.controls.fieldSchema.value).toEqual(['accession', 'length']);
    await showStep(2);
    expectCardSizes({CSV: '24 KB', TSV: '20 KB', JSON: '40 KB', EXCEL: '32 KB'});
  });

  it('shows an editable picker in CUSTOM and restores the custom subset after ALL', async () => {
    availableFields$.next(fields);
    await selectMode('CUSTOM');

    expect((fixture.nativeElement as HTMLElement).querySelector('#step-fields-title ~ .step-hint')).toBeNull();
    element<HTMLInputElement>('app-export-fields-picker input[type="checkbox"]').click();
    await fixture.whenStable();
    expect(component.form.controls.fieldSchema.value).toEqual(['length']);

    await selectMode('ALL');
    expect(component.form.controls.fieldSchema.value).toEqual(['accession', 'length']);
    expect((fixture.nativeElement as HTMLElement).querySelector('app-export-fields-picker')).toBeNull();

    await selectMode('CUSTOM');
    expect(component.form.controls.fieldSchema.value).toEqual(['length']);
    expect(element<HTMLInputElement>('app-export-fields-picker input[type="checkbox"]').checked).toBe(false);
    expect(element<HTMLInputElement>('app-export-fields-picker label:last-child input').checked).toBe(true);
  });

  it('preserves an intentionally empty custom selection across toggles', async () => {
    availableFields$.next(fields);
    await selectMode('CUSTOM');
    element<HTMLButtonElement>('app-export-fields-picker button:last-child').click();
    await fixture.whenStable();
    expect(component.form.controls.fieldSchema.hasError('required')).toBe(true);

    await selectMode('ALL');
    expect(component.form.controls.fieldSchema.value).toEqual(['accession', 'length']);
    await selectMode('CUSTOM');
    expect(component.form.controls.fieldSchema.value).toEqual([]);
  });

  it('does not overwrite CUSTOM when fields arrive after switching modes', async () => {
    await selectMode('CUSTOM');
    availableFields$.next(fields);
    await fixture.whenStable();
    expect(component.form.controls.fieldSchema.value).toEqual([]);

    element<HTMLInputElement>('app-export-fields-picker input[type="checkbox"]').click();
    await fixture.whenStable();
    availableFields$.next([...fields]);
    await fixture.whenStable();
    expect(component.form.controls.fieldSchema.value).toEqual(['accession']);

    await selectMode('ALL');
    expect(component.form.controls.fieldSchema.value).toEqual(['accession', 'length']);
  });

  it('shows a recoverable message if fields fail to load', async () => {
    availableFields$.error(new Error('Field metadata unavailable'));
    await fixture.whenStable();

    expect(element<HTMLElement>('#step-fields-title ~ [role="alert"]').textContent)
      .toContain('Available fields could not be loaded.');
    expect(component.form.controls.fieldSchema.value).toEqual([]);
  });

  it('cancels field loading when the wizard is destroyed', () => {
    expect(availableFields$.observed).toBe(true);
    fixture.destroy();
    expect(availableFields$.observed).toBe(false);
  });

  it('keeps previous/next navigation working through the required stepper query', async () => {
    const stepper = fixture.debugElement.query(By.directive(MatStepper)).injector.get(MatStepper);
    expect(stepper.selectedIndex).toBe(1);

    element<HTMLButtonElement>('.wizard-nav-steps button:first-child').click();
    await fixture.whenStable();
    expect(stepper.selectedIndex).toBe(0);

    element<HTMLButtonElement>('.wizard-nav-steps button:last-child').click();
    await fixture.whenStable();
    expect(stepper.selectedIndex).toBe(1);
  });

  it('reviews live filters as read-only chips and formats the existing search total', async () => {
    activeFilters.set(currentFilters);
    searchResult.set({content: [], page: 0, size: 50, totalElements: 12345, totalPages: 247});
    await reviewFilters();

    const chips = element<HTMLElement>('.filter-review-chips');
    expect(chips.textContent).toContain('kinase');
    expect(chips.textContent).toContain('Reviewed:');
    expect(chips.textContent).toContain('No');
    expect(chips.querySelector('button')).toBeNull();
    expect(chips.getAttribute('aria-label')).toBe('Current export filters (read-only)');
    expect(element<HTMLElement>('.row-count-value').textContent).toBe('12,345');
    expect(element<HTMLElement>('.row-count-card').getAttribute('aria-atomic')).toBe('true');
    expect((fixture.nativeElement as HTMLElement).querySelector('.filter-review-warning')).toBeNull();

    activeFilters.set({accession: 'P12345'});
    await fixture.whenStable();
    expect(chips.textContent).toContain('P12345');
    expect(chips.textContent).not.toContain('kinase');
  });

  it('shows the empty review and zero-result warning without saving a missing snapshot', async () => {
    await reviewFilters();
    expect(element<HTMLElement>('.filter-review-chips').textContent).toContain('No filters applied.');
    expect(element<HTMLElement>('.filter-review-warning').textContent).toContain('No data matches');
    expect(element<HTMLButtonElement>('.save-filter-action').disabled).toBe(true);
    element<HTMLButtonElement>('.save-filter-action').click();
    expect(openDialog).not.toHaveBeenCalled();
  });

  it('opens the existing name-only dialog without submitting, navigating or losing field selection', async () => {
    activeFilters.set(currentFilters);
    availableFields$.next(fields);
    await selectMode('CUSTOM');
    element<HTMLInputElement>('app-export-fields-picker input[type="checkbox"]').click();
    const submit = vi.fn();
    element<HTMLFormElement>('.wizard-form').addEventListener('submit', submit);
    await openSaveFilter();

    expect(openDialog).toHaveBeenCalledExactlyOnceWith(SaveFiltersDialogComponent, {width: '420px'});
    expect(element<HTMLButtonElement>('.save-filter-action').type).toBe('button');
    expect(element<HTMLButtonElement>('.save-filter-action').getAttribute('aria-haspopup')).toBe('dialog');
    expect(createSavedFilter).not.toHaveBeenCalled();
    await closeSaveDialog({name: savedFilter.name});
    savedFilter$.next(savedFilter);
    savedFilter$.complete();
    await fixture.whenStable();

    expect(submit).not.toHaveBeenCalled();
    expect(closeWizard).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
    expect(navigateByUrl).not.toHaveBeenCalled();
    expect(fixture.debugElement.query(By.directive(MatStepper)).injector.get(MatStepper).selectedIndex).toBe(0);
    expect(component.form.controls.fieldSchema.value).toEqual(['length']);
    element<HTMLButtonElement>('.wizard-nav-steps button:last-child').click();
    await fixture.whenStable();
    expect(element<HTMLInputElement>('input[value="CUSTOM"]').checked).toBe(true);
  });

  it('saves the current reviewed snapshot with the existing payload and success feedback', async () => {
    activeFilters.set(currentFilters);
    await openSaveFilter();
    // Changes while the naming dialog is open must not replace the reviewed snapshot.
    const laterFilters: GeneSearchRequest = {accession: 'P12345'};
    activeFilters.set(laterFilters);
    await closeSaveDialog({name: savedFilter.name});

    expect(createSavedFilter).toHaveBeenCalledExactlyOnceWith({name: 'Kinases', filterJson: currentFilters});
    expect(createSavedFilter.mock.calls[0]?.[0].filterJson).not.toBe(currentFilters);
    expect(openSnackBar).not.toHaveBeenCalled();
    expect(element<HTMLButtonElement>('.save-filter-action').disabled).toBe(true);
    element<HTMLButtonElement>('.save-filter-action').click();
    expect(openDialog).toHaveBeenCalledOnce();

    savedFilter$.next(savedFilter);
    savedFilter$.complete();
    await fixture.whenStable();
    expect(openSnackBar).toHaveBeenCalledExactlyOnceWith('Saved filter "Kinases"', 'Close', {
      duration: 4000, horizontalPosition: 'right', verticalPosition: 'top', panelClass: ['success-snackbar'],
    });
    expect(activeFilters()).toBe(laterFilters);
    expect(element<HTMLButtonElement>('.save-filter-action').disabled).toBe(false);
  });

  it('allows saving an explicitly unfiltered search without adding pagination or wizard fields', async () => {
    activeFilters.set({});
    await openSaveFilter();
    await closeSaveDialog({name: 'All data'});
    expect(createSavedFilter).toHaveBeenCalledExactlyOnceWith({name: 'All data', filterJson: {}});
  });

  it.each([null, undefined])('does nothing when the name dialog is dismissed with %s', async (result) => {
    activeFilters.set(currentFilters);
    await openSaveFilter();
    expect(element<HTMLButtonElement>('.save-filter-action').disabled).toBe(true);
    await closeSaveDialog(result);
    expect(createSavedFilter).not.toHaveBeenCalled();
    expect(openSnackBar).not.toHaveBeenCalled();
    expect(closeWizard).not.toHaveBeenCalled();
    expect(element<HTMLButtonElement>('.save-filter-action').disabled).toBe(false);
  });

  it.each([401, 403, 500])('uses existing failure feedback for HTTP %s without adding navigation', async (status) => {
    activeFilters.set(currentFilters);
    await openSaveFilter();
    await closeSaveDialog({name: savedFilter.name});
    savedFilter$.error(new HttpErrorResponse({status}));
    await fixture.whenStable();

    expect(openSnackBar).toHaveBeenCalledExactlyOnceWith('Failed to save filter "Kinases"', 'Retry', {
      duration: 6000, horizontalPosition: 'right', verticalPosition: 'top', panelClass: ['error-snackbar'],
    });
    expect(element<HTMLButtonElement>('.save-filter-action').disabled).toBe(false);
    expect(activeFilters()).toBe(currentFilters);
    expect(closeWizard).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
    expect(navigateByUrl).not.toHaveBeenCalled();
  });

  it('stops listening to the name dialog when the wizard is destroyed', async () => {
    activeFilters.set(currentFilters);
    await openSaveFilter();
    expect(dialogResult$.observed).toBe(true);
    fixture.destroy();
    expect(dialogResult$.observed).toBe(false);
    dialogResult$.next({name: savedFilter.name});
    expect(createSavedFilter).not.toHaveBeenCalled();
  });

  it('cancels an in-flight save when the wizard is destroyed', async () => {
    activeFilters.set(currentFilters);
    await openSaveFilter();
    await closeSaveDialog({name: savedFilter.name});
    expect(savedFilter$.observed).toBe(true);
    fixture.destroy();
    expect(savedFilter$.observed).toBe(false);
    savedFilter$.next(savedFilter);
    expect(openSnackBar).not.toHaveBeenCalled();
  });
});

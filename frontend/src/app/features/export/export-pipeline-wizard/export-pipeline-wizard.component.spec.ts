import {ComponentFixture, TestBed} from '@angular/core/testing';
import {signal} from '@angular/core';
import {By} from '@angular/platform-browser';
import {MatDialogRef} from '@angular/material/dialog';
import {MatStepper} from '@angular/material/stepper';
import {of, Subject} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ExportFieldSchema} from '@core/models/export-pipeline.model';
import {GenesStore} from '@features/genes/state/filters.store';
import {ExportPipelineService} from '@features/export/export-pipeline.service';
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

  function element<T extends Element>(selector: string): T {
    const root = fixture.nativeElement as HTMLElement;
    const result = root.querySelector<T>(selector);
    if (result === null) {
      throw new Error(`Expected element: ${selector}`);
    }
    return result;
  }

  async function selectMode(mode: 'ALL' | 'CUSTOM'): Promise<void> {
    element<HTMLInputElement>(`input[name="fieldMode"][value="${mode}"]`).click();
    await fixture.whenStable();
  }

  beforeEach(async () => {
    availableFields$ = new Subject<ExportFieldSchema[]>();
    getAvailableFields.mockReset().mockReturnValue(availableFields$.asObservable());

    await TestBed.configureTestingModule({
      imports: [ExportPipelineWizardComponent],
      providers: [
        {
          provide: GenesStore,
          useValue: {activeFilters: signal(null), searchResult: signal(null)},
        },
        {provide: MatDialogRef, useValue: {close: vi.fn()}},
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
    getAvailableFields.mockReturnValue(of(fields));
    fixture = TestBed.createComponent(ExportPipelineWizardComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();

    expect(component.form.controls.fieldSchema.value).toEqual(['accession', 'length']);
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
});

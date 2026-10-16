import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {GeneSearchRequest} from '@core/models/saved-filter.model';
import {ExportPipeline} from '@core/models/export-pipeline.model';
import {MatDialog} from '@angular/material/dialog';
import {Subject} from 'rxjs';
import {NotificationService} from '@shared/directive/notification.service';
import {ExportPipelineWizardComponent} from '@features/export/export-pipeline-wizard/export-pipeline-wizard.component';

import {ActiveFiltersComponent} from './active-filters.component';

describe('ActiveFiltersComponent', () => {
  let component: ActiveFiltersComponent;
  let fixture: ComponentFixture<ActiveFiltersComponent>;
  let result$: Subject<ExportPipeline | null | undefined>;
  const openDialog = vi.fn();
  const success = vi.fn<NotificationService['success']>();
  const error = vi.fn<NotificationService['error']>();

  async function openWizard(): Promise<void> {
    fixture.componentRef.setInput('filters', {accession: 'P12345'} satisfies GeneSearchRequest);
    await fixture.whenStable();
    const button = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.export-btn');
    if (button === null) throw new Error('Expected the export action');
    button.click();
    await fixture.whenStable();
  }

  beforeEach(async () => {
    result$ = new Subject<ExportPipeline | null | undefined>();
    openDialog.mockReset().mockReturnValue({afterClosed: () => result$.asObservable()});
    success.mockReset();
    error.mockReset();
    await TestBed.configureTestingModule({
      imports: [ActiveFiltersComponent],
      providers: [
        {provide: MatDialog, useValue: {open: openDialog}},
        {provide: NotificationService, useValue: {success, error}},
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ActiveFiltersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should return no chips when filters input is null', () => {
    fixture.componentRef.setInput('filters', null);
    fixture.detectChanges();

    expect(component.filtersChips()).toEqual([]);
  });

  it('should build chips for non-empty scalar and array filters', () => {
    const filters: GeneSearchRequest = {
      globalSearch: 'kinase',
      reviewed: true,
      taxid: 9606,
      evidenceLevels: [1, 2],
      keywords: ['membrane', 'transport']
    };

    fixture.componentRef.setInput('filters', filters);
    fixture.detectChanges();

    const chips = component.filtersChips();
    expect(chips.some((chip) => chip.label === 'Search' && chip.value === 'kinase')).toBe(true);
    expect(chips.some((chip) => chip.label === 'Reviewed' && chip.value === 'Yes')).toBe(true);
    expect(chips.some((chip) => chip.label === 'TaxID' && chip.value === '9606')).toBe(true);
    expect(chips.some((chip) => chip.label === 'Evidence' && chip.value === 'Protein level, Transcript level')).toBe(true);
    expect(chips.some((chip) => chip.label === 'Keywords' && chip.value.includes('membrane'))).toBe(true);
  });

  it('should map reviewed=false to No and keep zero values', () => {
    const filters: GeneSearchRequest = {
      reviewed: false,
      taxid: 0,
      lengthMin: 0
    };

    fixture.componentRef.setInput('filters', filters);
    fixture.detectChanges();

    const chips = component.filtersChips();
    expect(chips.some((chip) => chip.label === 'Reviewed' && chip.value === 'No')).toBe(true);
    expect(chips.some((chip) => chip.label === 'TaxID' && chip.value === '0')).toBe(true);
    expect(chips.some((chip) => chip.label === 'Length Min' && chip.value === '0')).toBe(true);
  });

  it('should emit filterRemoved when removeFilter is called', async () => {
    let removedKey: keyof GeneSearchRequest | undefined;
    component.filterRemoved.subscribe((key) => {
      removedKey = key;
    });

    component.removeFilter('accession');

    fixture.detectChanges();
    await fixture.whenStable();
    expect(removedKey).toBe('accession');
  });

  it('should emit chips count based on generated chips', async () => {
    const countSpy = vi.fn();
    component.setChipsCount.subscribe(countSpy);

    fixture.componentRef.setInput('filters', {
      accession: 'P12345',
      keywords: ['kinase']
    } satisfies GeneSearchRequest);
    fixture.detectChanges();

    component.filtersChips();

    await fixture.whenStable();
    expect(countSpy).toHaveBeenCalled();
    expect(countSpy).toHaveBeenLastCalledWith(2);
  });

  it('accepts the created pipeline rather than a legacy success flag', async () => {
    await openWizard();
    expect(openDialog).toHaveBeenCalledExactlyOnceWith(ExportPipelineWizardComponent, {
      width: '800px', maxWidth: '90vw',
    });
    const pipeline: ExportPipeline = {
      id: 19, name: 'Human kinases', format: 'CSV', fieldSchema: ['accession'],
      status: 'QUEUED', createdAt: '2026-01-01T00:00:00Z',
    };
    result$.next(pipeline);
    result$.complete();
    expect(success).toHaveBeenCalledExactlyOnceWith('The export pipeline "Human kinases" was successfully created.');
    expect(error).not.toHaveBeenCalled();
  });

  it.each([null, undefined])('does not notify on dialog cancellation %s', async (result) => {
    await openWizard();
    result$.next(result);
    result$.complete();
    expect(success).not.toHaveBeenCalled();
    expect(error).not.toHaveBeenCalled();
  });

  it('stops listening for dialog results when the caller is destroyed', async () => {
    await openWizard();
    expect(result$.observed).toBe(true);
    fixture.destroy();
    expect(result$.observed).toBe(false);
    expect(success).not.toHaveBeenCalled();
  });
});

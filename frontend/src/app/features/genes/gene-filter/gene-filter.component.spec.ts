import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {GeneFilterComponent} from './gene-filter.component';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {EVIDENCE_LEVELS} from '@core/models/protein.model';

describe('GeneFilterComponent', () => {
  let component: GeneFilterComponent;
  let fixture: ComponentFixture<GeneFilterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GeneFilterComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(GeneFilterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form controls including globalSearch', () => {
    expect(component.form.get('globalSearch')).toBeDefined();
    expect(component.form.get('accession')).toBeDefined();
    expect(component.form.get('goTermId')).toBeDefined();
    expect(component.form.get('keywords')).toBeDefined();
  });

  it('should enforce max length validators for documented fields', () => {
    component.form.controls.accession.setValue('a'.repeat(21));
    component.form.controls.geneNamePrimary.setValue('a'.repeat(101));
    component.form.controls.organism.setValue('a'.repeat(301));

    expect(component.form.controls.accession.hasError('maxlength')).toBe(true);
    expect(component.form.controls.geneNamePrimary.hasError('maxlength')).toBe(true);
    expect(component.form.controls.organism.hasError('maxlength')).toBe(true);
  });

  it('should emit filterChange with ranges mapped to min/max fields', async () => {
    let emitted: GeneFilterSnapshot | undefined;
    component.filterChange.subscribe((snapshot) => {
      emitted = snapshot;
    });

    component.form.patchValue({
      accession: 'P12345',
      length: {min: 100, max: 500},
      molecularWeight: {min: 10000, max: 50000}
    });

    component.applyFilters();

    fixture.detectChanges();
    await fixture.whenStable();

    expect(emitted?.accession).toBe('P12345');
    expect(emitted?.lengthMin).toBe(100);
    expect(emitted?.lengthMax).toBe(500);
    expect(emitted?.molecularWeightMin).toBe(10000);
    expect(emitted?.molecularWeightMax).toBe(50000);
  });

  it('should preserve globalSearch from incoming value when applying filters', async () => {
    fixture.componentRef.setInput('value', {
      globalSearch: 'kinase',
      accession: 'P12345'
    } satisfies GeneFilterSnapshot);
    fixture.detectChanges();

    let emitted: GeneFilterSnapshot | undefined;
    component.filterChange.subscribe((snapshot) => {
      emitted = snapshot;
    });

    component.form.controls.accession.setValue('Q8N158');
    component.applyFilters();

    fixture.detectChanges();
    await fixture.whenStable();

    expect(emitted?.globalSearch).toBe('kinase');
    expect(emitted?.accession).toBe('Q8N158');
  });

  it('should mark form touched and not emit when invalid', async () => {
    const markAllAsTouchedSpy = vi.spyOn(component.form, 'markAllAsTouched');
    let emitted = false;
    component.filterChange.subscribe(() => {
      emitted = true;
    });

    component.form.controls.goTermId.setValue('INVALID');
    component.applyFilters();

    fixture.detectChanges();
    await fixture.whenStable();

    expect(markAllAsTouchedSpy).toHaveBeenCalledOnce();
    expect(emitted).toBe(false);
  });

  it('should toggle evidence levels and emit updated snapshot', async () => {
    let emitted: GeneFilterSnapshot | undefined;
    component.filterChange.subscribe((snapshot) => {
      emitted = snapshot;
    });

    component.toggleEvidence(EVIDENCE_LEVELS[0]);

    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.form.controls.evidenceLevels.value).toContain(EVIDENCE_LEVELS[0]);
    expect(emitted?.evidenceLevels).toContain(EVIDENCE_LEVELS[0]);
  });

  it('should emit filterClear and reset defaults when clearAll is invoked', () => {
    const clearSpy = vi.fn();
    component.filterClear.subscribe(clearSpy);

    component.form.patchValue({
      accession: 'P12345',
      reviewed: true,
      taxid: 9606
    });

    // clearAll is protected and triggered by template button; direct invocation keeps behavior explicit.
    (component as unknown as { clearAll: () => void }).clearAll();

    expect(component.form.controls.accession.value).toBe('');
    expect(component.form.controls.reviewed.value).toBeNull();
    expect(component.form.controls.taxid.value).toBeNull();
    expect(clearSpy).toHaveBeenCalledOnce();
  });
});


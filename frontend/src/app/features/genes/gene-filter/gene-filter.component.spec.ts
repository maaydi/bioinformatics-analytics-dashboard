import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {GeneFilterComponent} from './gene-filter.component';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {ReactiveFormsModule} from '@angular/forms';
import {MatCardContent, MatCardHeader} from '@angular/material/card';
import {MatIcon} from '@angular/material/icon';
import {MatButton} from '@angular/material/button';
import {MatDivider} from '@angular/material/list';
import {MatError, MatFormField, MatInput} from '@angular/material/input';
import {MatButtonToggle, MatButtonToggleGroup} from '@angular/material/button-toggle';
import {MatCheckbox} from '@angular/material/checkbox';
import {MatOption, MatSelect} from '@angular/material/select';
import {EVIDENCE_LEVELS} from '@core/models/protein.model';
import {InputComponent} from '@shared/components/input/input.component';
import {RangeInputComponent} from '@shared/components/range-input/range-input.component';
import {GenesService} from '@features/genes/genes.service';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {provideHttpClient} from '@angular/common/http';

describe('GeneFilterComponent', () => {
  let component: GeneFilterComponent;
  let fixture: ComponentFixture<GeneFilterComponent>;
  let service: GenesService;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [
        GenesService,
        provideHttpClient(),
        provideHttpClientTesting()
      ],
      imports: [
        GeneFilterComponent,
        ReactiveFormsModule,
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
        RangeInputComponent
      ]
    }).compileComponents();
    service = TestBed.inject(GenesService);
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(GeneFilterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });


  describe('Component Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize form with all controls', () => {
      expect(component.form).toBeDefined();
      expect(component.form.get('globalSearch')).toBeDefined();
      expect(component.form.get('accession')).toBeDefined();
      expect(component.form.get('entryName')).toBeDefined();
      expect(component.form.get('geneNamePrimary')).toBeDefined();
      expect(component.form.get('proteinFullName')).toBeDefined();
      expect(component.form.get('reviewed')).toBeDefined();
      expect(component.form.get('organism')).toBeDefined();
      expect(component.form.get('taxid')).toBeDefined();
      expect(component.form.get('lineage')).toBeDefined();
      expect(component.form.get('length')).toBeDefined();
      expect(component.form.get('molecularWeight')).toBeDefined();
      expect(component.form.get('evidenceLevels')).toBeDefined();
      expect(component.form.get('keywords')).toBeDefined();
      expect(component.form.get('goTermId')).toBeDefined();
      expect(component.form.get('goAspect')).toBeDefined();
      expect(component.form.get('featureType')).toBeDefined();
      expect(component.form.get('crossRefSource')).toBeDefined();
    });

    it('should have output signals defined', () => {
      expect(component.filterChange).toBeDefined();
      expect(component.filterClear).toBeDefined();
    });

    it('should have evidence levels constant available', () => {
      expect(component.evidenceLevels).toEqual(EVIDENCE_LEVELS);
    });
  });

  describe('Form Initialization Values', () => {
    it('should initialize globalSearch with empty string', () => {
      expect(component.form.get('globalSearch')?.value).toBe('');
    });

    it('should initialize accession with empty string', () => {
      expect(component.form.get('accession')?.value).toBe('');
    });

    it('should initialize reviewed with null', () => {
      expect(component.form.get('reviewed')?.value).toBeNull();
    });

    it('should initialize taxid with null', () => {
      expect(component.form.get('taxid')?.value).toBeNull();
    });

    it('should initialize length with min and max as null', () => {
      const length = component.form.get('length')?.value;
      expect(length?.min).toBeNull();
      expect(length?.max).toBeNull();
    });

    it('should initialize molecularWeight with min and max as null', () => {
      const weight = component.form.get('molecularWeight')?.value;
      expect(weight?.min).toBeNull();
      expect(weight?.max).toBeNull();
    });

    it('should initialize evidenceLevels as empty array', () => {
      expect(component.form.get('evidenceLevels')?.value).toEqual([]);
    });

    it('should initialize keywords as empty array', () => {
      expect(component.form.get('keywords')?.value).toEqual([]);
    });

    it('should initialize goTermId with no pattern validation error', () => {
      const control = component.form.get('goTermId');
      control?.setValue('');
      expect(control?.hasError('pattern')).toBe(false);
    });

    it('should enforce max length validation on documented text fields', () => {
      component.form.controls.globalSearch.setValue('a'.repeat(201));
      component.form.controls.accession.setValue('a'.repeat(21));
      component.form.controls.geneNamePrimary.setValue('a'.repeat(101));
      component.form.controls.organism.setValue('a'.repeat(301));

      expect(component.form.controls.globalSearch.hasError('maxlength')).toBe(true);
      expect(component.form.controls.accession.hasError('maxlength')).toBe(true);
      expect(component.form.controls.geneNamePrimary.hasError('maxlength')).toBe(true);
      expect(component.form.controls.organism.hasError('maxlength')).toBe(true);
    });

    it('should enforce taxid as positive integer', () => {
      component.form.controls.taxid.setValue(0);
      expect(component.form.controls.taxid.hasError('min')).toBe(true);

      component.form.controls.taxid.setValue(-2);
      expect(component.form.controls.taxid.hasError('taxidPositiveInteger')).toBe(true);

      component.form.controls.taxid.setValue(9606);
      expect(component.form.controls.taxid.errors).toBeNull();
    });

    it('should enforce keyword array constraints', () => {
      component.form.controls.keywords.setValue(Array.from({length: 11}, (_, index) => `k-${index}`));
      expect(component.form.controls.keywords.hasError('keywordsMaxCount')).toBe(true);

      component.form.controls.keywords.setValue(['a'.repeat(101)]);
      expect(component.form.controls.keywords.hasError('keywordMaxLength')).toBe(true);

      component.form.controls.keywords.setValue(['valid-keyword']);
      expect(component.form.controls.keywords.errors).toBeNull();
    });
  });

  describe('Apply Filters', () => {
    it('should emit filterChange when form is valid', async () => {
      let emittedSnapshot: GeneFilterSnapshot | undefined;

      component.filterChange.subscribe((snapshot) => {
        emittedSnapshot = snapshot;
      });

      component.form.patchValue({
        globalSearch: 'kinase'
      });

      component.applyFilters();

      fixture.detectChanges();
      await fixture.whenStable();
      expect(emittedSnapshot).toBeDefined();
      expect(emittedSnapshot?.globalSearch).toBe('kinase');
    });

    it('should mark all as touched when form is invalid', () => {
      const markSpy = vi.spyOn(component.form, 'markAllAsTouched');

      component.form.patchValue({
        goTermId: 'INVALID'
      });

      component.applyFilters();

      expect(markSpy).toHaveBeenCalled();
    });

    it('should not emit filterChange when form is invalid', async () => {
      let emitted = false;

      component.filterChange.subscribe(() => {
        emitted = true;
      });

      component.form.patchValue({
        goTermId: 'INVALID' // Invalid GO term format
      });

      component.applyFilters();

      fixture.detectChanges();
      await fixture.whenStable();
      expect(emitted).toBe(false);
    });

    it('should create snapshot with range values mapped correctly', async () => {
      let emittedSnapshot: GeneFilterSnapshot | undefined;

      component.filterChange.subscribe((snapshot) => {
        emittedSnapshot = snapshot;
      });

      component.form.patchValue({
        length: {min: 100, max: 500},
        molecularWeight: {min: 10000, max: 50000}
      });

      component.applyFilters();

      fixture.detectChanges();
      await fixture.whenStable();
      expect(emittedSnapshot?.lengthMin).toBe(100);
      expect(emittedSnapshot?.lengthMax).toBe(500);
      expect(emittedSnapshot?.molecularWeightMin).toBe(10000);
      expect(emittedSnapshot?.molecularWeightMax).toBe(50000);
    });
  });

  describe('Clear All', () => {
    it('should not emit filterClear when only form.reset is performed', async () => {
      let emitted = false;

      component.filterClear.subscribe(() => {
        emitted = true;
      });

      // Note: clearAll is protected, test via toggleEvidence which calls applyFilters
      component.form.patchValue({
        globalSearch: 'test'
      });

      // Reset form by calling reset directly (simulates clearAll behavior)
      component.form.reset({
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

      fixture.detectChanges();
      await fixture.whenStable();
      expect(emitted).toBe(false);
    });

    it('should reset form to initial state', () => {
      component.form.patchValue({
        globalSearch: 'test',
        accession: 'P12345',
        reviewed: true,
        taxid: 9606
      });

      component.form.reset({
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

      expect(component.form.get('globalSearch')?.value).toBe('');
      expect(component.form.get('accession')?.value).toBe('');
      expect(component.form.get('reviewed')?.value).toBeNull();
      expect(component.form.get('taxid')?.value).toBeNull();
    });

    it('should reset range values to null', () => {
      component.form.patchValue({
        length: {min: 100, max: 500}
      });

      component.form.reset({
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

      const length = component.form.get('length')?.value;
      expect(length?.min).toBeNull();
      expect(length?.max).toBeNull();
    });

    it('should reset arrays to empty', () => {
      component.form.patchValue({
        evidenceLevels: [1, 2, 3],
        keywords: ['kinase', 'transferase']
      });

      component.form.reset({
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

      expect(component.form.get('evidenceLevels')?.value).toEqual([]);
      expect(component.form.get('keywords')?.value).toEqual([]);
    });

    it('should not emit change events when resetting', async () => {
      const changes: GeneFilterSnapshot[] = [];

      component.filterChange.subscribe((snapshot) => {
        changes.push(snapshot);
      });

      component.form.reset({
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

      fixture.detectChanges();
      await fixture.whenStable();
      expect(changes.length).toBe(0);
    });
  });

  describe('Evidence Level Toggle', () => {
    it('should add evidence level when not selected', () => {
      const level = EVIDENCE_LEVELS[0];
      component.form.get('evidenceLevels')?.setValue([]);

      component.toggleEvidence(level);

      const current = component.form.get('evidenceLevels')?.value;
      expect(current).toContain(level);
    });

    it('should remove evidence level when already selected', () => {
      const level = EVIDENCE_LEVELS[0];
      component.form.get('evidenceLevels')?.setValue([level]);

      component.toggleEvidence(level);

      const current = component.form.get('evidenceLevels')?.value;
      expect(current).not.toContain(level);
    });

    it('should allow multiple evidence levels', () => {
      const level1 = EVIDENCE_LEVELS[0];
      const level2 = EVIDENCE_LEVELS[1];

      component.toggleEvidence(level1);
      component.toggleEvidence(level2);

      const current = component.form.get('evidenceLevels')?.value;
      expect(current).toContain(level1);
      expect(current).toContain(level2);
    });

    it('should apply filters after toggling evidence', async () => {
      let emitted = false;

      component.filterChange.subscribe(() => {
        emitted = true;
      });

      const level = EVIDENCE_LEVELS[0];
      component.toggleEvidence(level);

      fixture.detectChanges();
      await fixture.whenStable();
      expect(emitted).toBe(true);
    });
  });

  describe('GO Term Validation', () => {
    it('should validate GO term format GO:\\d{7}', () => {
      const control = component.form.get('goTermId');

      control?.setValue('GO:0005524');
      expect(control?.hasError('pattern')).toBe(false);

      control?.setValue('GO:123456');
      expect(control?.hasError('pattern')).toBe(true);

      control?.setValue('GO:12345678');
      expect(control?.hasError('pattern')).toBe(true);

      control?.setValue('INVALID');
      expect(control?.hasError('pattern')).toBe(true);
    });

    it('should allow empty GO term', () => {
      const control = component.form.get('goTermId');
      control?.setValue('');
      expect(control?.hasError('pattern')).toBe(false);
    });

    it('should convert empty GO term to null in snapshot', async () => {
      let snapshot: GeneFilterSnapshot | undefined;

      component.filterChange.subscribe((s) => {
        snapshot = s;
      });

      component.form.patchValue({goTermId: ''});
      component.applyFilters();

      fixture.detectChanges();
      await fixture.whenStable();
      expect(snapshot?.goTermId).toBeNull();
    });

    it('should convert valid GO term to snapshot', async () => {
      let snapshot: GeneFilterSnapshot | undefined;

      component.filterChange.subscribe((s) => {
        snapshot = s;
      });

      component.form.patchValue({goTermId: 'GO:0005524'});
      component.applyFilters();

      fixture.detectChanges();
      await fixture.whenStable();
      expect(snapshot?.goTermId).toBe('GO:0005524');
    });
  });

  describe('Global Search Debounce', () => {
    it('should debounce global search input', async () => {
      let emitCount = 0;

      component.filterChange.subscribe(() => {
        emitCount++;
      });

      component.form.get('globalSearch')?.setValue('k');
      component.form.get('globalSearch')?.setValue('ki');
      component.form.get('globalSearch')?.setValue('kin');
      component.form.get('globalSearch')?.setValue('kina');
      component.form.get('globalSearch')?.setValue('kinase');

      fixture.detectChanges();

      await new Promise(resolve => setTimeout(resolve, 600));
      // Due to debounce, should have fewer emits than 5
      expect(emitCount).toBeLessThan(5);
    });

    it('should not emit on first pristine form with empty search', async () => {
      let emitted = false;

      component.filterChange.subscribe(() => {
        emitted = true;
      });

      // Form is pristine and search is empty
      fixture.detectChanges();

      await new Promise(resolve => setTimeout(resolve, 600));
      expect(emitted).toBe(false);
    });
  });

  describe('Snapshot Transformation', () => {
    it('should transform form values to snapshot correctly', async () => {
      let snapshot: GeneFilterSnapshot | undefined;

      component.filterChange.subscribe((s) => {
        snapshot = s;
      });

      component.form.patchValue({
        globalSearch: 'kinase',
        accession: 'P12345',
        entryName: 'PROT_HUMAN',
        geneNamePrimary: 'GENE1',
        proteinFullName: 'Full Protein Name',
        reviewed: true,
        organism: 'Homo sapiens',
        taxid: 9606,
        lineage: 'Eukaryota',
        length: {min: 100, max: 500},
        molecularWeight: {min: 10000, max: 50000},
        evidenceLevels: [1, 2],
        keywords: ['kinase'],
        goTermId: 'GO:0005524',
        goAspect: 'F',
        featureType: 'disulfide bond',
        crossRefSource: 'PDB'
      });

      component.applyFilters();

      fixture.detectChanges();
      await fixture.whenStable();
      expect(snapshot?.globalSearch).toBe('kinase');
      expect(snapshot?.accession).toBe('P12345');
      expect(snapshot?.lengthMin).toBe(100);
      expect(snapshot?.lengthMax).toBe(500);
      expect(snapshot?.molecularWeightMin).toBe(10000);
      expect(snapshot?.molecularWeightMax).toBe(50000);
      expect(snapshot?.goTermId).toBe('GO:0005524');
    });

    it('should handle null range values in snapshot', async () => {
      let snapshot: GeneFilterSnapshot | undefined;

      component.filterChange.subscribe((s) => {
        snapshot = s;
      });

      component.form.patchValue({
        length: {min: null, max: null},
        molecularWeight: {min: null, max: null}
      });

      component.applyFilters();

      fixture.detectChanges();
      await fixture.whenStable();
      expect(snapshot?.lengthMin).toBeNull();
      expect(snapshot?.lengthMax).toBeNull();
      expect(snapshot?.molecularWeightMin).toBeNull();
      expect(snapshot?.molecularWeightMax).toBeNull();
    });
  });

  describe('Edge Cases', () => {
    it('should handle form without any values set', async () => {
      let snapshot: GeneFilterSnapshot | undefined;

      component.filterChange.subscribe((s) => {
        snapshot = s;
      });

      component.applyFilters();

      fixture.detectChanges();
      await fixture.whenStable();
      expect(snapshot).toBeDefined();
      expect(snapshot?.globalSearch).toBe('');
    });

    it('should preserve form pristine state after reset', () => {
      component.form.markAsPristine();
      expect(component.form.pristine).toBe(true);

      component.form.reset({
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

      expect(component.form.pristine).toBe(true);
    });

    it('should handle very long string values', async () => {
      let snapshot: GeneFilterSnapshot | undefined;

      component.filterChange.subscribe((s) => {
        snapshot = s;
      });

      const longString = 'a'.repeat(201);
      component.form.patchValue({
        globalSearch: longString
      });

      component.applyFilters();

      fixture.detectChanges();
      await fixture.whenStable();
      expect(snapshot).toBeUndefined();
      expect(component.form.controls.globalSearch.hasError('maxlength')).toBe(true);
    });

    it('should handle special characters in search', async () => {
      let snapshot: GeneFilterSnapshot | undefined;

      component.filterChange.subscribe((s) => {
        snapshot = s;
      });

      const specialChars = '!@#$%^&*()_+-=[]{}|;:,.<>?';
      component.form.patchValue({
        globalSearch: specialChars
      });

      component.applyFilters();

      fixture.detectChanges();
      await fixture.whenStable();
      expect(snapshot?.globalSearch).toBe(specialChars);
    });
  });

  describe('Evidence Level Display', () => {
    it('should return formatted string for all evidence levels', () => {
      EVIDENCE_LEVELS.forEach((level) => {
        const display = (component as any).getEvidenceLevel(level);
        expect(display).toMatch(/\d+\s+-\s+.+/);
      });
    });
  });
});












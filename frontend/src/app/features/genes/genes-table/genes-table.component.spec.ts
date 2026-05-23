import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it} from 'vitest';
import {GenesTableComponent} from './genes-table.component';
import {PagedResponse} from '@core/models/paged-response.model';
import {ProteinSummary} from '@core/models/protein.model';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {MatTableModule} from '@angular/material/table';
import {MatChipsModule} from '@angular/material/chips';
import {MatIconModule} from '@angular/material/icon';
import {CommonModule} from '@angular/common';

describe('GenesTableComponent', () => {
  let component: GenesTableComponent;
  let fixture: ComponentFixture<GenesTableComponent>;

  const mockProteinData: ProteinSummary[] = [
    {
      id: 1,
      accession: 'P12345',
      entryName: 'PROT_HUMAN',
      proteinFullName: 'Test Protein 1',
      geneNamePrimary: 'GENE1',
      organismName: 'Homo sapiens',
      taxid: 9606,
      length: 150,
      molecularWeight: 15000,
      reviewed: true,
      evidenceLevel: 1,
      keywords: ['kinase', 'transferase']
    },
    {
      id: 2,
      accession: 'Q98765',
      entryName: 'PROT2_HUMAN',
      proteinFullName: 'Test Protein 2',
      geneNamePrimary: 'GENE2',
      organismName: 'Mus musculus',
      taxid: 10090,
      length: 250,
      molecularWeight: 25000,
      reviewed: false,
      evidenceLevel: 2,
      keywords: ['hydrolase']
    }
  ];

  const mockPagedResponse: PagedResponse<ProteinSummary> = {
    content: mockProteinData,
    page: 0,
    size: 10,
    totalElements: 2,
    totalPages: 1
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        GenesTableComponent,
        MatTableModule,
        MatChipsModule,
        MatIconModule,
        CommonModule
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(GenesTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('Component Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });


    it('should initialize inputs with default values', () => {
      expect(component.data()).toBeNull();
      expect(component.loading()).toBe(false);
      expect(component.errorMessage()).toBeNull();
      expect(component.filters()).toBeNull();
    });

    it('should have all output signals defined', () => {
      expect(component.sortChange).toBeDefined();
      expect(component.pageChange).toBeDefined();
      expect(component.rowClick).toBeDefined();
      expect(component.exportClick).toBeDefined();
    });

    it('should have correct displayed columns', () => {
      expect(component.displayedColumns).toEqual([
        'accession',
        'entryName',
        'proteinFullName',
        'organismName',
        'length',
        'reviewed',
        'evidenceLevel',
        'actions'
      ]);
    });
  });

  describe('Input Bindings', () => {
    it('should accept paged data', () => {
      fixture.componentRef.setInput('data', mockPagedResponse);
      fixture.detectChanges();

      expect(component.data()).toEqual(mockPagedResponse);
    });

    it('should accept loading state', () => {
      expect(component.loading()).toBe(false);
    });

    it('should accept error message', () => {
      expect(component.errorMessage()).toBeNull();
    });

    it('should accept filter snapshot', () => {
      expect(component.filters()).toBeNull();
    });
  });

  describe('Row Click Output', () => {
    it('should emit rowClick output with selected protein', async () => {
      const testProtein = mockProteinData[0];
      let emittedValue: ProteinSummary | undefined;

      component.rowClick.subscribe((protein: ProteinSummary) => {
        emittedValue = protein;
      });

      component.selectRowSummary(testProtein);

      fixture.detectChanges();
      await fixture.whenStable();
      expect(emittedValue).toEqual(testProtein);
    });

    it('should emit rowClick with correct protein data', async () => {
      const expectedProtein = mockProteinData[1];
      let receivedProtein: ProteinSummary | undefined;

      component.rowClick.subscribe((protein) => {
        receivedProtein = protein;
      });

      component.selectRowSummary(expectedProtein);

      fixture.detectChanges();
      await fixture.whenStable();
      expect(receivedProtein).toEqual(expectedProtein);
      expect(receivedProtein?.id).toBe(2);
    });
  });

  describe('Filter Chips Computed Signal', () => {
    it('should return empty array when filters is null', () => {
      fixture.componentRef.setInput('filters', null);
      fixture.detectChanges();

      expect(component.filtersChips()).toEqual([]);
    });

    it('should build chips for string filter values', () => {
      const mockFilters: GeneFilterSnapshot = {
        globalSearch: 'kinase',
        accession: '',
        entryName: '',
        geneNamePrimary: '',
        proteinFullName: '',
        reviewed: null,
        organism: '',
        taxid: null,
        lineage: '',
        lengthMin: null,
        lengthMax: null,
        molecularWeightMin: null,
        molecularWeightMax: null,
        evidenceLevels: [],
        keywords: [],
        goTermId: null,
        goAspect: null,
        featureType: '',
        crossRefSource: ''
      };

      fixture.componentRef.setInput('filters', mockFilters);
      fixture.detectChanges();

      const chips = component.filtersChips();
      expect(chips.length).toBeGreaterThan(0);
      expect(chips.some((chip) => chip.label === 'Search')).toBe(true);
    });

    it('should build chips for accession filter', () => {
      const mockFilters: GeneFilterSnapshot = {
        globalSearch: '',
        accession: 'P12345',
        entryName: '',
        geneNamePrimary: '',
        proteinFullName: '',
        reviewed: null,
        organism: '',
        taxid: null,
        lineage: '',
        lengthMin: null,
        lengthMax: null,
        molecularWeightMin: null,
        molecularWeightMax: null,
        evidenceLevels: [],
        keywords: [],
        goTermId: null,
        goAspect: null,
        featureType: '',
        crossRefSource: ''
      };

      fixture.componentRef.setInput('filters', mockFilters);
      fixture.detectChanges();

      const chips = component.filtersChips();
      expect(chips.some((chip) => chip.label === 'Accession')).toBe(true);
    });

    it('should skip empty string values', () => {
      const mockFilters: GeneFilterSnapshot = {
        globalSearch: '',
        accession: '',
        entryName: '',
        geneNamePrimary: '',
        proteinFullName: '',
        reviewed: null,
        organism: '',
        taxid: null,
        lineage: '',
        lengthMin: null,
        lengthMax: null,
        molecularWeightMin: null,
        molecularWeightMax: null,
        evidenceLevels: [],
        keywords: [],
        goTermId: null,
        goAspect: null,
        featureType: '',
        crossRefSource: ''
      };

      fixture.componentRef.setInput('filters', mockFilters);
      fixture.detectChanges();

      const chips = component.filtersChips();
      expect(chips.length).toBe(0);
    });

    it('should build chips for numeric values', () => {
      const mockFilters: GeneFilterSnapshot = {
        globalSearch: '',
        accession: '',
        entryName: '',
        geneNamePrimary: '',
        proteinFullName: '',
        reviewed: null,
        organism: '',
        taxid: 9606,
        lineage: '',
        lengthMin: 100,
        lengthMax: 500,
        molecularWeightMin: null,
        molecularWeightMax: null,
        evidenceLevels: [],
        keywords: [],
        goTermId: null,
        goAspect: null,
        featureType: '',
        crossRefSource: ''
      };

      fixture.componentRef.setInput('filters', mockFilters);
      fixture.detectChanges();

      const chips = component.filtersChips();
      expect(chips.some((chip) => chip.label === 'TaxID')).toBe(true);
      expect(chips.some((chip) => chip.label === 'Length Min')).toBe(true);
      expect(chips.some((chip) => chip.label === 'Length Max')).toBe(true);
    });

    it('should handle reviewed boolean as Yes/No', () => {
      const mockFilters: GeneFilterSnapshot = {
        globalSearch: '',
        accession: '',
        entryName: '',
        geneNamePrimary: '',
        proteinFullName: '',
        reviewed: true,
        organism: '',
        taxid: null,
        lineage: '',
        lengthMin: null,
        lengthMax: null,
        molecularWeightMin: null,
        molecularWeightMax: null,
        evidenceLevels: [],
        keywords: [],
        goTermId: null,
        goAspect: null,
        featureType: '',
        crossRefSource: ''
      };

      fixture.componentRef.setInput('filters', mockFilters);
      fixture.detectChanges();

      const chips = component.filtersChips();
      const reviewedChip = chips.find((chip) => chip.label === 'Reviewed');
      expect(reviewedChip?.value).toBe('Yes');
    });

    it('should handle array values by joining with comma', () => {
      const mockFilters: GeneFilterSnapshot = {
        globalSearch: '',
        accession: '',
        entryName: '',
        geneNamePrimary: '',
        proteinFullName: '',
        reviewed: null,
        organism: '',
        taxid: null,
        lineage: '',
        lengthMin: null,
        lengthMax: null,
        molecularWeightMin: null,
        molecularWeightMax: null,
        evidenceLevels: [1, 2, 3],
        keywords: ['kinase', 'transferase'],
        goTermId: null,
        goAspect: null,
        featureType: '',
        crossRefSource: ''
      };

      fixture.componentRef.setInput('filters', mockFilters);
      fixture.detectChanges();

      const chips = component.filtersChips();
      expect(chips.some((chip) => chip.label === 'Evidence')).toBe(true);
      expect(chips.some((chip) => chip.label === 'Keywords')).toBe(true);
    });

    it('should skip empty arrays', () => {
      const mockFilters: GeneFilterSnapshot = {
        globalSearch: '',
        accession: '',
        entryName: '',
        geneNamePrimary: '',
        proteinFullName: '',
        reviewed: null,
        organism: '',
        taxid: null,
        lineage: '',
        lengthMin: null,
        lengthMax: null,
        molecularWeightMin: null,
        molecularWeightMax: null,
        evidenceLevels: [],
        keywords: [],
        goTermId: null,
        goAspect: null,
        featureType: '',
        crossRefSource: ''
      };

      fixture.componentRef.setInput('filters', mockFilters);
      fixture.detectChanges();

      const chips = component.filtersChips();
      expect(chips.length).toBe(0);
    });
  });

  describe('Edge Cases', () => {
    it('should handle filters with all properties set', () => {
      const mockFilters: GeneFilterSnapshot = {
        globalSearch: 'search term',
        accession: 'P12345',
        entryName: 'ENTRY_HUMAN',
        geneNamePrimary: 'GENE1',
        proteinFullName: 'Full Protein Name',
        reviewed: true,
        organism: 'Homo sapiens',
        taxid: 9606,
        lineage: 'Eukaryota',
        lengthMin: 100,
        lengthMax: 500,
        molecularWeightMin: 10000,
        molecularWeightMax: 50000,
        evidenceLevels: [1],
        keywords: ['kinase'],
        goTermId: 'GO:0005524',
        goAspect: 'F',
        featureType: 'disulfide bond',
        crossRefSource: 'PDB'
      };

      fixture.componentRef.setInput('filters', mockFilters);
      fixture.detectChanges();

      const chips = component.filtersChips();
      expect(chips.length).toBeGreaterThan(0);
    });

    it('should handle reviewed false value', () => {
      const mockFilters: GeneFilterSnapshot = {
        globalSearch: '',
        accession: '',
        entryName: '',
        geneNamePrimary: '',
        proteinFullName: '',
        reviewed: false,
        organism: '',
        taxid: null,
        lineage: '',
        lengthMin: null,
        lengthMax: null,
        molecularWeightMin: null,
        molecularWeightMax: null,
        evidenceLevels: [],
        keywords: [],
        goTermId: null,
        goAspect: null,
        featureType: '',
        crossRefSource: ''
      };

      fixture.componentRef.setInput('filters', mockFilters);
      fixture.detectChanges();

      const chips = component.filtersChips();
      const reviewedChip = chips.find((chip) => chip.label === 'Reviewed');
      expect(reviewedChip?.value).toBe('No');
    });

    it('should handle multiple evidence levels', () => {
      const mockFilters: GeneFilterSnapshot = {
        globalSearch: '',
        accession: '',
        entryName: '',
        geneNamePrimary: '',
        proteinFullName: '',
        reviewed: null,
        organism: '',
        taxid: null,
        lineage: '',
        lengthMin: null,
        lengthMax: null,
        molecularWeightMin: null,
        molecularWeightMax: null,
        evidenceLevels: [1, 2, 3, 4, 5],
        keywords: [],
        goTermId: null,
        goAspect: null,
        featureType: '',
        crossRefSource: ''
      };

      fixture.componentRef.setInput('filters', mockFilters);
      fixture.detectChanges();

      const chips = component.filtersChips();
      const evidenceChip = chips.find((chip) => chip.label === 'Evidence');
      expect(evidenceChip?.value).toContain('1');
      expect(evidenceChip?.value).toContain('5');
    });

    it('should handle zero values in numeric fields', () => {
      const mockFilters: GeneFilterSnapshot = {
        globalSearch: '',
        accession: '',
        entryName: '',
        geneNamePrimary: '',
        proteinFullName: '',
        reviewed: null,
        organism: '',
        taxid: 0,
        lineage: '',
        lengthMin: 0,
        lengthMax: 0,
        molecularWeightMin: 0,
        molecularWeightMax: 0,
        evidenceLevels: [],
        keywords: [],
        goTermId: null,
        goAspect: null,
        featureType: '',
        crossRefSource: ''
      };

      fixture.componentRef.setInput('filters', mockFilters);
      fixture.detectChanges();

      const chips = component.filtersChips();
      // Zero values should be included
      expect(chips.some((chip) => chip.label === 'TaxID')).toBe(true);
    });

    it('should handle goTermId with valid format', () => {
      const mockFilters: GeneFilterSnapshot = {
        globalSearch: '',
        accession: '',
        entryName: '',
        geneNamePrimary: '',
        proteinFullName: '',
        reviewed: null,
        organism: '',
        taxid: null,
        lineage: '',
        lengthMin: null,
        lengthMax: null,
        molecularWeightMin: null,
        molecularWeightMax: null,
        evidenceLevels: [],
        keywords: [],
        goTermId: 'GO:0005524',
        goAspect: null,
        featureType: '',
        crossRefSource: ''
      };

      fixture.componentRef.setInput('filters', mockFilters);
      fixture.detectChanges();

      const chips = component.filtersChips();
      expect(chips.some((chip) => chip.label === 'Go ID')).toBe(true);
    });

    it('should react to filter changes dynamically', () => {
      let mockFilters: GeneFilterSnapshot = {
        globalSearch: '',
        accession: '',
        entryName: '',
        geneNamePrimary: '',
        proteinFullName: '',
        reviewed: null,
        organism: '',
        taxid: null,
        lineage: '',
        lengthMin: null,
        lengthMax: null,
        molecularWeightMin: null,
        molecularWeightMax: null,
        evidenceLevels: [],
        keywords: [],
        goTermId: null,
        goAspect: null,
        featureType: '',
        crossRefSource: ''
      };

      fixture.componentRef.setInput('filters', mockFilters);
      fixture.detectChanges();
      expect(component.filtersChips().length).toBe(0);

      mockFilters = {...mockFilters, globalSearch: 'test'};
      fixture.componentRef.setInput('filters', mockFilters);
      fixture.detectChanges();
      expect(component.filtersChips().length).toBeGreaterThan(0);
    });
  });
});






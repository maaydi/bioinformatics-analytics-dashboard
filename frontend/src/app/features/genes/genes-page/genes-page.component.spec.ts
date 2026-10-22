import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {GenesPageComponent} from './genes-page.component';
import {Router} from '@angular/router';
import {ProteinSummary} from '@core/models/protein.model';
import {GenesStore} from '@features/genes/state/filters.store';
import {GeneSearchRequest} from '@core/models/saved-filter.model';
import {GenesService} from '@features/genes/genes.service';
import {NotificationService} from '@shared/directive/notification.service';
import {of} from 'rxjs';

describe('GenesPageComponent', () => {
  let component: GenesPageComponent;
  let fixture: ComponentFixture<GenesPageComponent>;
  const navigateMock = vi.fn(() => Promise.resolve(true));

  beforeEach(async () => {
    const mockGenesService = {
      searchGenes: vi.fn().mockReturnValue(of({
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
      })),
      loadKeywords: vi.fn().mockReturnValue(of(['Kinase', 'Receptor', 'Membrane'])),
    };

    const mockNotificationService = {
      success: vi.fn(),
      error: vi.fn(),
      show: vi.fn().mockReturnValue({dismiss: vi.fn()}),
    };

    await TestBed.configureTestingModule({
      imports: [GenesPageComponent],
      providers: [
        {
          provide: Router,
          useValue: {
            navigate: navigateMock,
          },
        },
        {
          provide: GenesService,
          useValue: mockGenesService,
        },
        {
          provide: NotificationService,
          useValue: mockNotificationService,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GenesPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should expose store instance', () => {
    expect(component.store).toBeDefined();
  });

  it('should render filter, global search, active filters and table containers', () => {
    const host = fixture.nativeElement as HTMLElement;

    expect(host.querySelector('app-gene-filter')).toBeTruthy();
    expect(host.querySelector('app-global-search')).toBeTruthy();
    expect(host.querySelector('app-active-filters')).toBeTruthy();
    expect(host.querySelector('app-genes-table')).toBeTruthy();
  });

  it('should keep store reference stable across change detection', () => {
    const firstStore = component.store;

    fixture.detectChanges();
    fixture.detectChanges();

    expect(component.store).toBe(firstStore);
  });

  it('should trigger initial unfiltered search on creation', () => {
    const searchSpy = vi.spyOn(component.store, 'searchGene');
    const localFixture = TestBed.createComponent(GenesPageComponent);
    localFixture.detectChanges();

    expect(searchSpy).toHaveBeenCalledWith({});
  });

  it('should trigger initial search with preloaded active filters on creation', () => {
    const filters: GeneSearchRequest = {evidenceLevels: [1]};
    const localStore = TestBed.inject(GenesStore);
    vi.spyOn(localStore, 'activeFilters').mockReturnValue(filters);
    const searchSpy = vi.spyOn(localStore, 'searchGene').mockImplementation((() => {
    }) as any);
    const localFixture = TestBed.createComponent(GenesPageComponent);
    localFixture.detectChanges();

    expect(searchSpy).toHaveBeenCalledWith(filters);
  });

  it('should navigate to gene detail when a row is clicked', () => {
    const selectSpy = vi.spyOn(component.store, 'selectGeneSummary');
    const row: ProteinSummary = {
      id: 42,
      accession: 'P12345',
      entryName: 'TEST_HUMAN',
      proteinFullName: 'Protein',
      geneNamePrimary: 'GENE1',
      organismName: 'Homo sapiens',
      taxid: 9606,
      reviewed: true,
      length: 321,
      molecularWeight: 12345,
      evidenceLevel: 1,
      keywords: ['Kinase'],
    };

    component.openGeneDetails(row);

    expect(selectSpy).toHaveBeenCalledWith(row);
    expect(navigateMock).toHaveBeenCalledWith(['/genes', 'P12345']);
  });

  it('should retry search with active filters', () => {
    const searchSpy = vi.spyOn(component.store, 'searchGene');
    const filters = {organism: 'Homo sapiens'};
    vi.spyOn(component.store, 'activeFilters').mockReturnValue(filters);

    component.retrySearch();

    expect(searchSpy).toHaveBeenCalledWith(filters);
  });

  describe('Filter Management', () => {
    it('should apply filters when applyFilters is called', () => {
      const searchSpy = vi.spyOn(component.store, 'searchGene');
      const snapshot: GeneSearchRequest = {evidenceLevels: [1, 2]};

      component.applyFilters(snapshot);

      expect(searchSpy).toHaveBeenCalledWith(snapshot);
    });

    it('should clear filters and search empty when clearFilters is called', () => {
      const clearSpy = vi.spyOn(component.store, 'clearFilters');
      const searchSpy = vi.spyOn(component.store, 'searchGene');

      component.clearFilters();

      expect(clearSpy).toHaveBeenCalled();
      expect(searchSpy).toHaveBeenCalledWith({});
    });

    it('should apply multiple filter types in snapshot', () => {
      const searchSpy = vi.spyOn(component.store, 'searchGene');
      const complexSnapshot: GeneSearchRequest = {
        globalSearch: 'insulin',
        evidenceLevels: [1, 2],
        organism: 'Homo sapiens',
      };

      component.applyFilters(complexSnapshot);

      expect(searchSpy).toHaveBeenCalledWith(complexSnapshot);
    });

    it('should apply empty snapshot as filter clear', () => {
      const searchSpy = vi.spyOn(component.store, 'searchGene');
      const emptySnapshot: GeneSearchRequest = {};

      component.applyFilters(emptySnapshot);

      expect(searchSpy).toHaveBeenCalledWith(emptySnapshot);
    });
  });

  describe('Gene Detail Navigation', () => {
    it('should include row ID in navigation path', () => {
      const row: ProteinSummary = {
        id: 42,
        accession: 'P12345',
        entryName: 'TEST_HUMAN',
        proteinFullName: 'Protein',
        geneNamePrimary: 'GENE1',
        organismName: 'Homo sapiens',
        taxid: 9606,
        reviewed: true,
        length: 321,
        molecularWeight: 12345,
        evidenceLevel: 1,
        keywords: ['Kinase'],
      };

      component.openGeneDetails(row);

      expect(navigateMock).toHaveBeenCalledWith(['/genes', 'P12345']);
    });

    it('should select gene summary before navigation', () => {
      const selectSpy = vi.spyOn(component.store, 'selectGeneSummary');
      const row: ProteinSummary = {
        id: 123,
        accession: 'P54321',
        entryName: 'OTHER_HUMAN',
        proteinFullName: 'Other Protein',
        geneNamePrimary: 'GENE2',
        organismName: 'Homo sapiens',
        taxid: 9606,
        reviewed: false,
        length: 456,
        molecularWeight: 54321,
        evidenceLevel: 2,
        keywords: [],
      };

      component.openGeneDetails(row);

      expect(selectSpy).toHaveBeenCalledWith(row);
      expect(navigateMock).toHaveBeenCalled();
    });
  });

  describe('Component State', () => {
    it('should maintain store reference across detection cycles', () => {
      const firstStore = component.store;
      fixture.detectChanges();
      const secondStore = component.store;

      expect(firstStore).toBe(secondStore);
    });

  });

  describe('Store Integration', () => {
    it('should expose store as public readonly property', () => {
      expect(component.store).toBeDefined();
    });

    it('should use store methods for filter operations', () => {
      const clearSpy = vi.spyOn(component.store, 'clearFilters');
      component.clearFilters();
      expect(clearSpy).toHaveBeenCalled();
    });

    it('should use store methods for gene selection', () => {
      const selectSpy = vi.spyOn(component.store, 'selectGeneSummary');
      const row: ProteinSummary = {
        id: 1,
        accession: 'P00001',
        entryName: 'TEST',
        proteinFullName: 'Test',
        geneNamePrimary: 'TEST',
        organismName: 'Test',
        taxid: 1,
        reviewed: false,
        length: 1,
        molecularWeight: 1,
        evidenceLevel: 1,
        keywords: [],
      };

      component.openGeneDetails(row);
      expect(selectSpy).toHaveBeenCalledWith(row);
    });
  });
});

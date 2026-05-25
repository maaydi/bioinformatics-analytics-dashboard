import {TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {of, throwError} from 'rxjs';
import {GenesStore} from './filters.store';
import {GenesService} from '@features/genes/genes.service';
import {PagedResponse} from '@core/models/paged-response.model';
import {ProteinSummary} from '@core/models/protein.model';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';

describe('GenesStore', () => {
  type GenesStoreContract = {
    activeFilters: () => GeneFilterSnapshot | null;
    chipsCount: () => number;
    searchResult: () => PagedResponse<ProteinSummary> | null;
    onErrorMessage: () => string | null;
    selectedGene: () => ProteinSummary | null;
    loading: () => boolean;
    selectGeneSummary: (protein: ProteinSummary) => void;
    updateChipsCount: (value: number) => void;
    clearFilters: () => void;
    removeFilter: (key: keyof GeneFilterSnapshot) => void;
    searchGene: (snapshot: GeneFilterSnapshot) => void;
  };

  let store: GenesStoreContract;
  let genesServiceMock: { searchGenes: ReturnType<typeof vi.fn> };

  const summary: ProteinSummary = {
    id: 1,
    accession: 'P12345',
    entryName: 'GENE_HUMAN',
    proteinFullName: 'Protein Name',
    geneNamePrimary: 'GENE1',
    organismName: 'Homo sapiens',
    taxid: 9606,
    reviewed: true,
    length: 300,
    molecularWeight: 34000,
    evidenceLevel: 1,
    keywords: ['Kinase']
  };

  const response: PagedResponse<ProteinSummary> = {
    content: [summary],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1
  };

  beforeEach(() => {
    genesServiceMock = {
      searchGenes: vi.fn().mockReturnValue(of(response))
    };

    TestBed.configureTestingModule({
      providers: [{provide: GenesService, useValue: genesServiceMock}]
    });

    store = TestBed.inject(GenesStore) as unknown as GenesStoreContract;
  });

  it('should initialize with default state', () => {
    expect(store.activeFilters()).toBeNull();
    expect(store.chipsCount()).toBe(0);
    expect(store.searchResult()).toBeNull();
    expect(store.onErrorMessage()).toBeNull();
    expect(store.selectedGene()).toBeNull();
    expect(store.loading()).toBe(false);
  });

  it('should update selected gene using selectGeneSummary', () => {
    store.selectGeneSummary(summary);

    expect(store.selectedGene()).toEqual(summary);
  });

  it('should update chips count only for non-negative values', () => {
    store.updateChipsCount(4);
    expect(store.chipsCount()).toBe(4);

    store.updateChipsCount(-1);
    expect(store.chipsCount()).toBe(4);
  });

  it('should clear filters and result state via clearFilters', () => {
    const snapshot: GeneFilterSnapshot = {globalSearch: 'kinase'};
    store.searchGene(snapshot);
    store.selectGeneSummary(summary);

    store.clearFilters();

    expect(store.activeFilters()).toBeNull();
    expect(store.searchResult()).toBeNull();
    expect(store.selectedGene()).toBeNull();
    expect(store.onErrorMessage()).toBeNull();
    expect(store.loading()).toBe(false);
  });

  it('should search genes and store result on success', () => {
    const snapshot: GeneFilterSnapshot = {
      globalSearch: 'kinase',
      reviewed: true
    };

    store.searchGene(snapshot);

    expect(genesServiceMock.searchGenes).toHaveBeenCalledWith({
      globalSearch: 'kinase',
      reviewed: true,
      page: 0,
      size: 20,
      sort: 'id',
      direction: 'asc'
    });
    expect(store.activeFilters()).toEqual(snapshot);
    expect(store.searchResult()).toEqual(response);
    expect(store.onErrorMessage()).toBeNull();
    expect(store.loading()).toBe(false);
  });

  it('should set error message when search fails', () => {
    genesServiceMock.searchGenes.mockReturnValueOnce(
      throwError(() => new Error('search failed'))
    );

    store.searchGene({globalSearch: 'broken'});

    expect(store.onErrorMessage()).toBe(
      'Failed to search genes. Please contact the administrator for help.'
    );
    expect(store.loading()).toBe(false);
  });

  it('should return without action when removeFilter is called with no active filters', () => {
    const searchSpy = vi.spyOn(store, 'searchGene');

    store.removeFilter('accession');

    expect(searchSpy).not.toHaveBeenCalled();
  });

  it('should clear all state when removing the last active filter', () => {
    store.searchGene({accession: 'P12345'});
    const clearSpy = vi.spyOn(store, 'clearFilters');

    store.removeFilter('accession');

    expect(clearSpy).toHaveBeenCalledOnce();
    expect(store.activeFilters()).toBeNull();
  });

  it('should remove one filter and trigger a refreshed search when filters remain', () => {
    store.searchGene({accession: 'P12345', reviewed: true});
    const searchSpy = vi.spyOn(store, 'searchGene');

    store.removeFilter('accession');

    expect(searchSpy).toHaveBeenCalledWith({
      accession: null,
      reviewed: true
    });
  });

  it('should reset array filters to empty array when removing them', () => {
    store.searchGene({keywords: ['Kinase'], reviewed: true});
    const searchSpy = vi.spyOn(store, 'searchGene');

    store.removeFilter('keywords');

    expect(searchSpy).toHaveBeenCalledWith({
      keywords: [],
      reviewed: true
    });
  });
});


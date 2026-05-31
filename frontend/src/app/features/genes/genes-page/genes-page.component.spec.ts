import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {GenesPageComponent} from './genes-page.component';
import {Router} from '@angular/router';
import {ProteinSummary} from '@core/models/protein.model';
import {GenesStore} from '@features/genes/state/filters.store';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';

describe('GenesPageComponent', () => {
  let component: GenesPageComponent;
  let fixture: ComponentFixture<GenesPageComponent>;
  const navigateMock = vi.fn(() => Promise.resolve(true));

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GenesPageComponent],
      providers: [
        {
          provide: Router,
          useValue: {
            navigate: navigateMock,
          },
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
    const filters: GeneFilterSnapshot = {evidenceLevels: [1]};
    const localStore = TestBed.inject(GenesStore);
    vi.spyOn(localStore, 'activeFilters').mockReturnValue(filters);
    const searchSpy = vi.spyOn(localStore, 'searchGene');

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
    expect(navigateMock).toHaveBeenCalledWith(['/genes', 42]);
  });

  it('should retry search with active filters', () => {
    const searchSpy = vi.spyOn(component.store, 'searchGene');
    const filters = {organism: 'Homo sapiens'};
    vi.spyOn(component.store, 'activeFilters').mockReturnValue(filters);

    component.retrySearch();

    expect(searchSpy).toHaveBeenCalledWith(filters);
  });
});

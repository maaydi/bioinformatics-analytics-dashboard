import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it} from 'vitest';
import {PagedResponse} from '@core/models/paged-response.model';
import {ProteinSummary} from '@core/models/protein.model';
import {GenesTableComponent} from './genes-table.component';

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
      keywords: ['kinase']
    }
  ];

  const mockPagedResponse: PagedResponse<ProteinSummary> = {
    content: mockProteinData,
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GenesTableComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(GenesTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create with expected defaults', () => {
    expect(component).toBeTruthy();
    expect(component.data()).toBeNull();
    expect(component.loading()).toBe(false);
    expect(component.errorMessage()).toBeNull();
    expect(component.chipsCount()).toBe(0);
  });

  it('should emit selected row on selectRowSummary', async () => {
    let selected: ProteinSummary | undefined;
    component.rowClick.subscribe((protein) => {
      selected = protein;
    });

    component.selectRowSummary(mockProteinData[0]);

    fixture.detectChanges();
    await fixture.whenStable();
    expect(selected).toEqual(mockProteinData[0]);
  });

  it('should show loading state while loading is true', () => {
    fixture.componentRef.setInput('loading', true);
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.textContent).toContain('Loading gene results');
  });

  it('should show error state when there is no data and an error message', () => {
    fixture.componentRef.setInput('loading', false);
    fixture.componentRef.setInput('errorMessage', 'Search failed');
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    const error = host.querySelector('.error-msg');
    expect(error?.textContent).toContain('Search failed');
  });

  it('should show empty state when no rows are available', () => {
    fixture.componentRef.setInput('loading', false);
    fixture.componentRef.setInput('data', null);
    fixture.componentRef.setInput('chipsCount', 1);
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.textContent).toContain('No gene results yet');
  });

  it('should render table rows when data exists and chipsCount is greater than zero', () => {
    fixture.componentRef.setInput('data', mockPagedResponse);
    fixture.componentRef.setInput('chipsCount', 1);
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('table')).toBeTruthy();
    expect(host.textContent).toContain('P12345');
    expect(host.textContent).toContain('Yes');
  });

  it('should not render table when chipsCount is zero even if data exists', () => {
    fixture.componentRef.setInput('data', mockPagedResponse);
    fixture.componentRef.setInput('chipsCount', 0);
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('table')).toBeNull();
    expect(host.textContent).toContain('No gene results yet');
  });
});






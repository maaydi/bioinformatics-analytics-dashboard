import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it} from 'vitest';
import {Component, EventEmitter, Input, Output} from '@angular/core';
import {AgGridAngular} from 'ag-grid-angular';
import {SortChangedEvent} from 'ag-grid-community';
import {PagedResponse} from '@core/models/paged-response.model';
import {ProteinSummary} from '@core/models/protein.model';
import {GenesTableComponent} from './genes-table.component';

@Component({
  selector: 'ag-grid-angular',
  template: '',
})
class MockAgGridAngularComponent {
  @Input() columnDefs: unknown;
  @Input() defaultColDef: unknown;
  @Input() rowData: unknown;
  @Input() rowSelection: unknown;
  @Input() animateRows: unknown;
  @Input() suppressCellFocus: unknown;
  @Input() suppressRowClickSelection: unknown;
  @Output() readonly rowClicked = new EventEmitter<unknown>();
  @Output() readonly sortChanged = new EventEmitter<unknown>();
}

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
    })
      .overrideComponent(GenesTableComponent, {
        remove: {
          imports: [AgGridAngular],
        },
        add: {
          imports: [MockAgGridAngularComponent],
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(GenesTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create with expected defaults', () => {
    expect(component).toBeTruthy();
    expect(component.data()).toBeNull();
    expect(component.loading()).toBe(false);
    expect(component.errorMessage()).toBeNull();
    expect(component.rows()).toEqual([]);
  });

  it('should emit selected row on grid row click', async () => {
    let selected: ProteinSummary | undefined;
    component.rowClick.subscribe((protein) => {
      selected = protein;
    });

    component.onGridRowClicked({data: mockProteinData[0]} as never);

    fixture.detectChanges();
    await fixture.whenStable();
    expect(selected).toEqual(mockProteinData[0]);
  });

  it('should show loading state while loading is true', () => {
    fixture.componentRef.setInput('loading', true);
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('.skeleton-row')).toBeTruthy();
  });

  it('should show error state when there is no data and an error message', () => {
    fixture.componentRef.setInput('loading', false);
    fixture.componentRef.setInput('errorMessage', 'Search failed');
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    const error = host.querySelector('.error-msg');
    expect(error?.textContent).toContain('Search failed');
    expect(host.textContent).toContain('Retry');
  });

  it('should show empty state message when no rows are available', () => {
    fixture.componentRef.setInput('loading', false);
    fixture.componentRef.setInput('data', {
      content: [],
      page: 0,
      size: 50,
      totalElements: 0,
      totalPages: 0
    } satisfies PagedResponse<ProteinSummary>);
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.textContent).toContain('No proteins found');
  });

  it('should render AG Grid when data exists', () => {
    fixture.componentRef.setInput('data', mockPagedResponse);
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('ag-grid-angular')).toBeTruthy();
  });

  it('should reset sort to id asc when no active sorted column remains', () => {
    let emitted: { page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' } | undefined;
    component.updateSortDirection.subscribe((payload) => {
      emitted = payload;
    });

    const sortEvent = {
      api: {
        getColumnState: () => [{colId: 'accession', sort: null}],
      },
    } as unknown as SortChangedEvent<ProteinSummary>;

    component.onGridSortChanged(sortEvent);

    expect(emitted).toEqual({sort: 'id', direction: 'asc', page: 0});
  });

  it('should emit sort change payload for ascending sort', () => {
    let emitted: { page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' } | undefined;
    component.updateSortDirection.subscribe((payload) => {
      emitted = payload;
    });

    const sortEvent = {
      api: {
        getColumnState: () => [{colId: 'organismName', sort: 'asc'}],
      },
    } as unknown as SortChangedEvent<ProteinSummary>;

    component.onGridSortChanged(sortEvent);

    expect(emitted).toEqual({sort: 'organismName', direction: 'asc', page: 0});
  });

  it('should emit sort change payload for descending sort', () => {
    let emitted: { page?: number; size?: number; sort?: string; direction?: 'asc' | 'desc' } | undefined;
    component.updateSortDirection.subscribe((payload) => {
      emitted = payload;
    });

    const sortEvent = {
      api: {
        getColumnState: () => [{colId: 'length', sort: 'desc'}],
      },
    } as unknown as SortChangedEvent<ProteinSummary>;

    component.onGridSortChanged(sortEvent);

    expect(emitted).toEqual({sort: 'length', direction: 'desc', page: 0});
  });

  it('should emit retry click when retrySearch is called', () => {
    let called = false;
    component.retryClick.subscribe(() => {
      called = true;
    });

    component.retrySearch();

    expect(called).toBe(true);
  });
});






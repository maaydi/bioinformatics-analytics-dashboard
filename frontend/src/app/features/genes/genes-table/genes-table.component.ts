import {ChangeDetectionStrategy, Component, computed, inject, input, output} from '@angular/core';
import {DOCUMENT} from '@angular/common';
import {AgGridAngular} from 'ag-grid-angular';
import {
  AllCommunityModule,
  ColDef,
  GetRowIdParams,
  GridReadyEvent,
  GridSizeChangedEvent,
  ITooltipParams,
  ModuleRegistry,
  RowClickedEvent,
  SortChangedEvent,
  ValueGetterParams
} from 'ag-grid-community';
import {PagedResponse} from '@core/models/paged-response.model';
import {ProteinSummary} from '@core/models/protein.model';
import {GeneFilterPageSort} from '@core/models/saved-filter.model';
import {GENES_TABLE_MESSAGES} from '@features/genes/genes.messages';

ModuleRegistry.registerModules([AllCommunityModule]);


/**
 * Presentational table component for gene search results.
 *
 * Inputs:
 * - `data`: current paged response to render.
 * - `errorMessage`: user-facing error text to display when loading fails.
 * - `loading`: true while data is being fetched.
 * - `filters`: active filter snapshot used to render summary chips.
 *
 * Outputs:
 * - `sortChange`: emits `{ field, direction }` when sort changes.
 * - `pageChange`: emits `{ page, size }` when pagination changes.
 * - `rowClick`: emits the selected `ProteinSummary` row.
 * - `exportClick`: emits when CSV export is requested.
 */
@Component({
  selector: 'app-genes-table',
  imports: [AgGridAngular],
  templateUrl: './genes-table.component.html',
  styleUrl: './genes-table.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenesTableComponent {
  protected readonly messages = GENES_TABLE_MESSAGES;
  private readonly document = inject(DOCUMENT);
  readonly data = input<PagedResponse<ProteinSummary> | null>(null);
  readonly errorMessage = input<string | null>(null);
  readonly loading = input(true);
  readonly chipsCount = input<number>(0);

  readonly rows = computed(() => this.data()?.content ?? []);
  readonly hasRows = computed(() => this.rows().length > 0);
  readonly hasError = computed(() => Boolean(this.errorMessage()));
  readonly isDataLoaded = computed(() => this.data() !== null);
  readonly isEmpty = computed(() => this.isDataLoaded()
    && !this.loading()
    && !this.hasError()
    && !this.hasRows());
  readonly columnDefs: ColDef<ProteinSummary>[] = [
    {
      field: 'accession',
      headerName: this.messages.accession,
      sortable: true,
      minWidth: 80,
      sortIndex: 0,
      tooltipField: 'accession',
      cellRenderer: ({value}: { value: string }) => {
        return this.createCell('accession-col', value);
      }
    },
    {
      field: 'geneNamePrimary',
      headerName: this.messages.geneName,
      sortable: true,
      minWidth: 100,
      valueGetter: (params: ValueGetterParams<ProteinSummary>) => params.data?.geneNamePrimary ?? '-',
      tooltipValueGetter: (params: ITooltipParams<ProteinSummary>) => params.data?.geneNamePrimary ?? '-',
    },
    {
      field: 'proteinFullName',
      headerName: this.messages.proteinName,
      sortable: true,
      minWidth: 100,
      valueGetter: (params: ValueGetterParams<ProteinSummary>) => params.data?.proteinFullName ?? '-',
      tooltipValueGetter: (params: ITooltipParams<ProteinSummary>) => params.data?.proteinFullName ?? '-',
    },
    {
      field: 'organismName',
      headerName: this.messages.organism,
      sortable: true,
      minWidth: 100,
      tooltipField: 'organismName',
    },
    {
      field: 'length',
      headerName: this.messages.length,
      sortable: true,
      minWidth: 104,
      type: 'numericColumn',
      cellClass: 'numeric-cell',
    },
    {
      field: 'reviewed',
      headerName: this.messages.reviewed,
      sortable: true,
      minWidth: 120,
      cellClass: 'badge-cell',

      cellRenderer: ({value}: { value: boolean }) => this.createCell(
        `review-badge ${value ? 'is-reviewed' : 'not-reviewed'}`,
        value ? this.messages.yes : this.messages.no,
      ),
    },

    {
      field: 'evidenceLevel',
      headerName: this.messages.evidenceLevel,
      sortable: true,
      minWidth: 152,
      type: 'numericColumn',
      cellClass: ['badge-cell'],

      cellRenderer: ({value}: { value: number }) =>
        this.createCell(`evidence-badge level-${value}`, String(value ?? this.messages.missingValue)),
    },
    {
      field: 'keywords',
      headerName: this.messages.keywords,
      sortable: false,
      minWidth: 150,
      valueFormatter: (params) => params.value?.join(', ') ?? '-',
      cellRenderer: ({value}: { value: string[] }) => {
        if (!Array.isArray(value) || value.length === 0) {
          return this.createCell('empty-value', this.messages.missingValue);
        }
        const visible = value.slice(0, 2);
        const container = this.document.createElement('div');
        container.className = 'keywords-cell';
        for (const keyword of visible) {
          container.append(this.createCell('keyword-chip', keyword));
        }
        if (value.length > 2) {
          const more = this.createCell('keyword-chip more-counter-chip', this.messages.more(value.length - 2));
          more.title = value.slice(2).join(', ');
          container.append(more);
        }
        return container;
      },
    },
  ];

  readonly getRowId = (params: GetRowIdParams<ProteinSummary>) => String(params.data.id);

  readonly defaultColDef: ColDef<ProteinSummary> = {
    resizable: true,
    unSortIcon: true,
    suppressMovable: true,
    cellClass: 'text-truncate',
    wrapText: false,
    wrapHeaderText: false,
  };

  readonly rowSelection = {
    mode: 'singleRow' as const,
    enableClickSelection: false,
  };

  readonly updateSortDirection = output<GeneFilterPageSort>();
  readonly rowClick = output<ProteinSummary>();
  readonly retryClick = output<void>();

  private createCell(className: string, text: string): HTMLSpanElement {
    const cell = this.document.createElement('span');
    cell.className = className;
    cell.textContent = text;
    return cell;
  }

  onGridRowClicked(event: RowClickedEvent<ProteinSummary>): void {
    if (event.data) {
      this.rowClick.emit(event.data);
    }
  }

  onGridSortChanged(event: SortChangedEvent<ProteinSummary>): void {
    const sortedColumn = event.api.getColumnState().find((state) => state.sort === 'asc' || state.sort === 'desc');
    if (!sortedColumn?.colId || !sortedColumn.sort) {
      this.updateSortDirection.emit({sort: 'id', direction: 'asc', page: 0});
      return;
    }

    this.updateSortDirection.emit({
      sort: sortedColumn.colId,
      direction: sortedColumn.sort,
      page: 0,
    });
  }

  onGridReady(event: GridReadyEvent<ProteinSummary>): void {
    event.api.sizeColumnsToFit();
  }

  onGridSizeChanged(event: GridSizeChangedEvent<ProteinSummary>): void {
    event.api.sizeColumnsToFit();
  }

  retrySearch(): void {
    this.retryClick.emit();
  }

}

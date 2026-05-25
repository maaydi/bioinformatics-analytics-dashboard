import {ChangeDetectionStrategy, Component, computed, input, output} from '@angular/core';
import {AgGridAngular} from 'ag-grid-angular';
import {AllCommunityModule, ColDef, ModuleRegistry, RowClickedEvent, SortChangedEvent} from 'ag-grid-community';
import {PagedResponse} from '@core/models/paged-response.model';
import {ProteinSummary} from '@core/models/protein.model';
import {GeneFilterPageSort} from '@core/models/saved-filter.model';

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
  readonly data = input<PagedResponse<ProteinSummary> | null>(null);
  readonly errorMessage = input<string | null>(null);
  readonly loading = input(false);
  readonly chipsCount = input<number>(0);

  readonly rows = computed(() => this.data()?.content ?? []);
  readonly hasRows = computed(() => this.rows().length > 0);
  readonly hasError = computed(() => Boolean(this.errorMessage()));

  readonly columnDefs: ColDef<ProteinSummary>[] = [
    {
      field: 'accession',
      headerName: 'Accession',
      sortable: true,
      minWidth: 140,
      sort: 'asc',
      sortIndex: 0,
    },
    {
      field: 'geneNamePrimary',
      headerName: 'Gene Name',
      sortable: true,
      minWidth: 160,
      valueFormatter: ({value}) => value ?? '-',
    },
    {
      field: 'proteinFullName',
      headerName: 'Protein Name',
      sortable: true,
      minWidth: 220,
      valueFormatter: ({value}) => value ?? '-',
    },
    {
      field: 'organismName',
      headerName: 'Organism',
      sortable: true,
      minWidth: 220,
    },
    {
      field: 'length',
      headerName: 'Length',
      sortable: true,
      minWidth: 110,
      type: 'numericColumn',
    },
    {
      field: 'reviewed',
      headerName: 'Reviewed',
      sortable: true,
      minWidth: 130,
      valueFormatter: ({value}) => (value ? 'Reviewed' : 'Unreviewed'),
    },
    {
      field: 'evidenceLevel',
      headerName: 'Evidence Level',
      sortable: true,
      minWidth: 150,
      type: 'numericColumn',
    },
    {
      field: 'keywords',
      headerName: 'Keywords',
      sortable: false,
      minWidth: 260,
      valueFormatter: ({value}) => {
        if (!Array.isArray(value) || value.length === 0) {
          return '-';
        }
        if (value.length <= 3) {
          return value.join(', ');
        }
        return `${value.slice(0, 3).join(', ')} +${value.length - 3} more`;
      },
    },
  ];

  readonly defaultColDef: ColDef<ProteinSummary> = {
    resizable: true,
    unSortIcon: true,
    suppressMovable: true,
    flex: 1,
  };

  readonly rowSelection = {
    mode: 'singleRow' as const,
    enableClickSelection: false,
  };

  readonly updateSortDirection = output<GeneFilterPageSort>();
  readonly rowClick = output<ProteinSummary>();
  readonly retryClick = output<void>();

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

  retrySearch(): void {
    this.retryClick.emit();
  }

}

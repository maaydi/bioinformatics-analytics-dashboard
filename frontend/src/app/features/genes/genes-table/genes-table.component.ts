import {ChangeDetectionStrategy, Component, input, output} from '@angular/core';
import {PagedResponse} from '@core/models/paged-response.model';
import {ProteinSummary} from '@core/models/protein.model';
import {GeneFilterPageSort, GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell,
  MatHeaderCellDef,
  MatHeaderRow,
  MatHeaderRowDef,
  MatRow,
  MatRowDef,
  MatTable
} from '@angular/material/table';
import {MatIcon} from '@angular/material/icon';
import {
  CustomHeaderSortComponent,
  SortDirection,
  SortExchangeEvent
} from '@shared/components/custom-header-sort/custom-header-sort.component';
import {MatChip, MatChipSet} from '@angular/material/chips';
import {SlicePipe} from '@angular/common';
import {MatTooltip} from '@angular/material/tooltip';


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
  imports: [
    MatTable,
    MatColumnDef,
    MatHeaderCell,
    MatHeaderCellDef,
    MatCellDef,
    MatCell,
    MatIcon,
    MatHeaderRow,
    MatRow,
    MatHeaderRowDef,
    MatRowDef,
    CustomHeaderSortComponent,
    MatChipSet,
    MatChip,
    SlicePipe,
    MatTooltip
  ],
  templateUrl: './genes-table.component.html',
  styleUrl: './genes-table.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenesTableComponent {
  currentSortField: string = 'id';
  currentSortDirection: SortDirection = 'asc';
  readonly data = input<PagedResponse<ProteinSummary> | null>(null);
  readonly chipsCount = input<number>(0);
  readonly errorMessage = input<String | null>(null);
  readonly loading = input(false);
  readonly filters = input<GeneFilterSnapshot | null>(null);
  readonly updateSortDirection = output<GeneFilterPageSort>();
  readonly displayedColumns = [
    'accession', 'geneNamePrimary', 'proteinFullName', 'organismName', 'length', 'reviewed', 'evidenceLevel', 'keywords', 'actions'
  ];

  readonly sortChange = output<{ field: string; direction: 'asc' | 'desc' }>();
  readonly pageChange = output<{ page: number; size: number }>();
  readonly rowClick = output<ProteinSummary>();
  readonly exportClick = output<void>();

  /** Emits the selected row to the container for navigation/details handling. */
  selectRowSummary(row: ProteinSummary): void {
    this.rowClick.emit(row);
  }

  onSortChange(event: SortExchangeEvent): void {
    this.currentSortField = event.field;
    this.currentSortDirection = event.direction;
    this.updateSortDirection.emit({
      sort: this.currentSortField,
      direction: this.currentSortDirection === 'desc' ? 'desc' : 'asc'
    });
  }


}

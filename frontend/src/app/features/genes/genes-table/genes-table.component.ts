import {ChangeDetectionStrategy, Component, computed, input, output} from '@angular/core';
import {PagedResponse} from '@core/models/paged-response.model';
import {ProteinSummary} from '@core/models/protein.model';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {MatChip, MatChipRemove, MatChipSet} from '@angular/material/chips';
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
import {NgClass} from '@angular/common';

type FilterChip = { key: keyof GeneFilterSnapshot, label: string, value: string };

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
    MatChipSet,
    MatChip,
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
    NgClass,
    MatChipRemove
  ],
  templateUrl: './genes-table.component.html',
  styleUrl: './genes-table.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenesTableComponent {
  readonly data = input<PagedResponse<ProteinSummary> | null>(null);
  readonly errorMessage = input<String | null>(null);
  readonly loading = input(false);

  readonly filters = input<GeneFilterSnapshot | null>(null);
  readonly filterRemoved = output<keyof GeneFilterSnapshot>();
  readonly filtersChips = computed(() => this.buildFiltersChips(this.filters()));
  readonly displayedColumns = [
    'accession', 'entryName', 'proteinFullName', 'organismName', 'length', 'reviewed', 'evidenceLevel', 'actions'
  ];

  readonly sortChange = output<{ field: string; direction: 'asc' | 'desc' }>();
  readonly pageChange = output<{ page: number; size: number }>();
  readonly rowClick = output<ProteinSummary>();
  readonly exportClick = output<void>();

  /** Emits the selected row to the container for navigation/details handling. */
  selectRowSummary(row: ProteinSummary): void {
    this.rowClick.emit(row);
  }

  removeFilter(key: keyof GeneFilterSnapshot): void {
    this.filterRemoved.emit(key);
  }

  /** Converts non-empty filter fields into display chips. */
  private buildFiltersChips(filters: GeneFilterSnapshot | null): FilterChip[] {
    if (!filters) {
      return [];
    }
    const config: Array<{ key: keyof GeneFilterSnapshot; label: string }> = [
      {key: 'globalSearch', label: 'Search'},
      {key: 'accession', label: 'Accession'},
      {key: 'entryName', label: 'Entry'},
      {key: 'geneNamePrimary', label: 'Gene '},
      {key: 'proteinFullName', label: 'Protein'},
      {key: 'reviewed', label: 'Reviewed'},
      {key: 'organism', label: 'Organism'},
      {key: 'taxid', label: 'TaxID'},
      {key: 'lineage', label: 'Lineage'},
      {key: 'lengthMin', label: 'Length Min'},
      {key: 'lengthMax', label: 'Length Max'},
      {key: 'molecularWeightMin', label: 'Weight Min'},
      {key: 'molecularWeightMax', label: 'Weight Max'},
      {key: 'evidenceLevels', label: 'Evidence'},
      {key: 'keywords', label: 'Keywords'},
      {key: 'goTermId', label: 'Go ID'},
      {key: 'goAspect', label: 'Go Aspect'},
      {key: 'featureType', label: 'Feature'},
      {key: 'crossRefSource', label: 'CrossRef'},

    ];
    const chips: FilterChip[] = [];
    for (const item of config) {
      const rawValue = filters[item.key];
      if (rawValue === null || rawValue === undefined || rawValue === '') {
        continue;
      }
      if (Array.isArray(rawValue)) {
        if (rawValue.length === 0) {
          continue;
        }
        chips.push({key: item.key, label: item.label, value: rawValue.join(', ')});
        continue;
      }
      if (item.key === 'reviewed') {
        chips.push({key: item.key, label: item.label, value: rawValue ? 'Yes' : 'No'});
        continue;
      }
      chips.push({key: item.key, label: item.label, value: String(rawValue)});
    }
    return chips;
  }


}

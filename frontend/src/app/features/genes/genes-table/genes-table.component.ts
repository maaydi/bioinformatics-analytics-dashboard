import {ChangeDetectionStrategy, Component, computed, input, output} from '@angular/core';
import {PagedResponse} from '@core/models/paged-response.model';
import {ProteinSummary} from '@core/models/protein.model';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {MatChip, MatChipSet} from '@angular/material/chips';
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

type FilterChip = { label: string, value: string };

/**
 * Presentational (dumb) component — renders the AG Grid protein table.
 *
 * Columns (documentation/overview.md §5.B):
 *   Accession, Gene Name, Protein Name, Organism, Length, Reviewed, Evidence, Keywords
 *
 * Features (US-4, US-5, US-6):
 * - Server-side pagination
 * - Server-side sorting (click header)
 * - Row click → navigate to gene detail
 * - Column hide/show
 *
 * Inputs:
 *  - data: page of ProteinSummary items from parent
 *  - loading: spinner flag
 *
 * Outputs:
 *  - sortChange: {field, direction} when sort header clicked
 *  - pageChange: {page, size} when paginator changes
 *  - rowClick: protein id when a row is clicked
 *  - exportClick: when export CSV is triggered
 *
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
    NgClass
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
  readonly filtersChips = computed(() => this.buildFiltersChips(this.filters()));
  readonly displayedColumns = [
    'accession', 'entryName', 'proteinFullName', 'organismName', 'length', 'reviewed', 'evidenceLevel', 'actions'
  ];

  readonly sortChange = output<{ field: string; direction: 'asc' | 'desc' }>();
  readonly pageChange = output<{ page: number; size: number }>();
  readonly rowClick = output<ProteinSummary>();
  readonly exportClick = output<void>();

  selectRowSummary(row: ProteinSummary): void {
    this.rowClick.emit(row);
  }

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
        chips.push({label: item.label, value: rawValue.join(', ')});
        continue;
      }
      if (item.key === 'reviewed') {
        chips.push({label: item.label, value: rawValue ? 'Yes' : 'No'});
        continue;
      }
      chips.push({label: item.label, value: String(rawValue)});
    }
    return chips;
  }


}

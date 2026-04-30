import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { PagedResponse } from '../../../core/models/paged-response.model';
import { ProteinSummary } from '../../../core/models/protein.model';

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
 * TODO: implement in ticket GENE-001
 */
@Component({
  selector: 'app-genes-table',
  imports: [],
  templateUrl: './genes-table.component.html',
  styleUrl: './genes-table.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenesTableComponent {
  readonly data = input<PagedResponse<ProteinSummary> | null>(null);
  readonly loading = input(false);

  readonly sortChange = output<{ field: string; direction: 'asc' | 'desc' }>();
  readonly pageChange = output<{ page: number; size: number }>();
  readonly rowClick = output<number>();
  readonly exportClick = output<void>();
}

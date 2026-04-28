import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProteinSummary } from '../../../core/models/protein.model';
import { PagedResponse } from '../../../core/models/paged-response.model';

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
 * @Input  data    — page of ProteinSummary items from parent
 * @Output sortChange — emits {field, direction} when sort header clicked
 * @Output pageChange — emits {page, size} when paginator changes
 * @Output rowClick   — emits protein id when a row is clicked
 *
 * TODO: implement in ticket GENE-001
 */
@Component({
  selector: 'app-genes-table',
  standalone: true,
  imports: [CommonModule],
  template: `<p>AG Grid table — TODO: implement</p>`,
})
export class GenesTableComponent {
  @Input() data: PagedResponse<ProteinSummary> | null = null;
  @Input() loading = false;

  @Output() sortChange  = new EventEmitter<{ field: string; direction: 'asc' | 'desc' }>();
  @Output() pageChange  = new EventEmitter<{ page: number; size: number }>();
  @Output() rowClick    = new EventEmitter<number>();
  @Output() exportClick = new EventEmitter<void>();
}

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatTabsModule } from '@angular/material/tabs';

/**
 * Gene Detail page — Epic 5 (US-15, US-16, US-17).
 *
 * Displays a single protein entry with tabs:
 *   Summary | Sequence | Features | Cross References | Taxonomy | Publications | Similar Proteins
 *
 * Route: /genes/:id  (id bound via input() with withComponentInputBinding)
 * Data source: GET /api/genes/{id} → ProteinDetail
 * NFR: ≤ 1 s load time (§12.1)
 *
 * TODO: implement in ticket DETAIL-001
 */
@Component({
  selector: 'app-gene-detail',
  imports: [MatTabsModule],
  templateUrl: './gene-detail.component.html',
  styleUrl: './gene-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GeneDetailComponent {
  // Route param `id` will be bound via input() with withComponentInputBinding
  // readonly id = input.required<number>();
}

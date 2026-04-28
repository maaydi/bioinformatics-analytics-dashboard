import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTabsModule } from '@angular/material/tabs';

/**
 * Gene Detail page — Epic 5 (US-15, US-16, US-17).
 *
 * Displays a single protein entry with tabs:
 *   Summary | Sequence | Features | Cross References | Taxonomy | Publications | Similar Proteins
 *
 * Route: /genes/:id  (id bound via @Input via withComponentInputBinding)
 * Data source: GET /api/genes/{id} → ProteinDetail
 * NFR: ≤ 1 s load time (§12.1)
 *
 * TODO: implement in ticket DETAIL-001
 */
@Component({
  selector: 'app-gene-detail',
  standalone: true,
  imports: [CommonModule, MatTabsModule],
  template: `
    <h1>Gene Detail — TODO</h1>
    <mat-tab-group>
      <mat-tab label="Summary"><!-- TODO --></mat-tab>
      <mat-tab label="Sequence"><!-- TODO --></mat-tab>
      <mat-tab label="Features"><!-- TODO --></mat-tab>
      <mat-tab label="Cross References"><!-- TODO --></mat-tab>
      <mat-tab label="Taxonomy"><!-- TODO --></mat-tab>
      <mat-tab label="Publications"><!-- TODO --></mat-tab>
      <mat-tab label="Similar Proteins"><!-- TODO --></mat-tab>
    </mat-tab-group>
  `,
})
export class GeneDetailComponent {
  // Route param id will be bound here via withComponentInputBinding
  // @Input() id!: number;
}

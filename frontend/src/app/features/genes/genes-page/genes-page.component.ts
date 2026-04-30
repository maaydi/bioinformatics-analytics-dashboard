import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Gene Explorer page — container component (smart).
 * Epic 2 (US-4 to US-6), Epic 3 (US-7 to US-10), Epic 6 (US-18).
 *
 * Responsibilities:
 * - Holds reactive filter state
 * - Coordinates GenesTableComponent (dumb) and GeneFilterComponent (dumb)
 * - Calls GenesService for data
 * - Handles loading / error / empty states
 *
 * TODO: implement in ticket GENE-001
 */
@Component({
  selector: 'app-genes-page',
  imports: [],
  templateUrl: './genes-page.component.html',
  styleUrl: './genes-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenesPageComponent {}

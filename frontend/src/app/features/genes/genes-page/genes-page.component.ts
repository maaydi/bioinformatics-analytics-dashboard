import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {MatCard} from '@angular/material/card';
import {GeneFilterComponent} from '@features/genes/gene-filter/gene-filter.component';
import {GenesTableComponent} from '@features/genes/genes-table/genes-table.component';
import {GenesStore} from '@features/genes/state/filters.store';

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
 */
@Component({
  selector: 'app-genes-page',
  imports: [
    MatCard,
    GeneFilterComponent,
    GenesTableComponent
  ],
  providers: [GenesStore],
  templateUrl: './genes-page.component.html',
  styleUrl: './genes-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenesPageComponent {
  readonly store = inject(GenesStore);
}

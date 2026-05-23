import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {MatCard} from '@angular/material/card';
import {GeneFilterComponent} from '@features/genes/gene-filter/gene-filter.component';
import {GenesTableComponent} from '@features/genes/genes-table/genes-table.component';
import {GenesStore} from '@features/genes/state/filters.store';

/**
 * Container component for the Genes feature.
 *
 * Behavior:
 * - Owns a local `GenesStore` instance.
 * - Connects filter interactions and table rendering through shared store state.
 * - Delegates API calls and state transitions to the store.
 */
@Component({
  selector: 'app-genes-page',
  imports: [
    MatCard,
    GeneFilterComponent,
    GenesTableComponent
  ],
  templateUrl: './genes-page.component.html',
  styleUrl: './genes-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenesPageComponent {
  readonly store = inject(GenesStore);
}

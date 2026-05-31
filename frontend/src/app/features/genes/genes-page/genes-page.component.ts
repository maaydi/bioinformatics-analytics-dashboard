import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {Router} from '@angular/router';
import {MatCard} from '@angular/material/card';
import {GeneFilterComponent} from '@features/genes/gene-filter/gene-filter.component';
import {GlobalSearchComponent} from '@features/genes/global-search/global-search.component';
import {GenesTableComponent} from '@features/genes/genes-table/genes-table.component';
import {GenesStore} from '@features/genes/state/filters.store';
import {ActiveFiltersComponent} from '@features/genes/active-filters/active-filters.component';
import {ResultHeaderComponent} from '@features/genes/result-header/result-header.component';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {ProteinSummary} from '@core/models/protein.model';

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
    GlobalSearchComponent,
    GenesTableComponent,
    ActiveFiltersComponent,
    ResultHeaderComponent
  ],
  templateUrl: './genes-page.component.html',
  styleUrl: './genes-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenesPageComponent {
  readonly store = inject(GenesStore);
  private readonly router = inject(Router);

  constructor() {
    this.store.searchGene(this.store.activeFilters() ?? {});
  }

  applyFilters(snapshot: GeneFilterSnapshot): void {
    this.store.searchGene(snapshot);
  }

  clearFilters(): void {
    this.store.clearFilters();
    this.store.searchGene({});
  }

  retrySearch(): void {
    this.store.searchGene(this.store.activeFilters() ?? {});
  }

  openGeneDetails(row: ProteinSummary): void {
    this.store.selectGeneSummary(row);
    void this.router.navigate(['/genes', row.id]);
  }
}

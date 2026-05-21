import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {MatCard} from '@angular/material/card';
import {GeneFilterComponent} from '@features/genes/gene-filter/gene-filter.component';
import {GenesService} from '@features/genes/genes.service';
import {PagedResponse} from '@core/models/paged-response.model';
import {ProteinSummary} from '@core/models/protein.model';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {GenesTableComponent} from '@features/genes/genes-table/genes-table.component';

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
  templateUrl: './genes-page.component.html',
  styleUrl: './genes-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenesPageComponent {
  readonly activeFilters = signal<GeneFilterSnapshot | null>(null);
  readonly searchResult = signal<PagedResponse<ProteinSummary> | null>(null);
  readonly onErrorMessage = signal<String | null>(null);
  readonly selectedGene = signal<ProteinSummary | null>(null);
  readonly loading = signal(false);
  private readonly geneService = inject(GenesService);

  searchGene(snapshot: GeneFilterSnapshot): void {
    this.activeFilters.set(snapshot);
    this.selectedGene.set(null);
    this.loading.set(true);

    this.geneService.searchGenes(
      {
        ...snapshot,
        page: 0,
        size: 20,
        sort: 'id',
        direction: 'asc'
      }
    ).subscribe(
      {
        next: (result) => {
          this.searchResult.set(result);
          this.onErrorMessage.set(null);
          this.loading.set(false);

        },
        error: () => {
          this.onErrorMessage.set('Failed to search genes. Please contact the administrator for help.');
          this.loading.set(false);
        }
      }
    );
  }

  selectGeneSummary(protein: ProteinSummary): void {
    this.selectedGene.set(protein);
  }
}

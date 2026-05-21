import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {MatCard} from '@angular/material/card';
import {GeneFilterComponent} from '@features/genes/gene-filter/gene-filter.component';
import {GenesService} from '@features/genes/genes.service';
import {PagedResponse} from '@core/models/paged-response.model';
import {ProteinSummary} from '@core/models/protein.model';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';

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
    GeneFilterComponent
  ],
  templateUrl: './genes-page.component.html',
  styleUrl: './genes-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenesPageComponent {
  readonly activeFilters = signal<GeneFilterSnapshot | null>(null);
  readonly searchResult = signal<PagedResponse<ProteinSummary> | null>(null);
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
          this.loading.set(false);
          console.log('Gene search result ', result);

        },
        error: (err: unknown) => {
          this.loading.set(false);
          console.error(err);
        }
      }
    );
  }

  selectGeneSummary(protein: ProteinSummary): void {
    this.selectedGene.set(protein);
  }


}

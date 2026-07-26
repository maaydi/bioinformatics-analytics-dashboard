import {ChangeDetectionStrategy, Component, inject, PLATFORM_ID, signal} from '@angular/core';
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
import {NotificationService} from '@shared/directive/notification.service';
import {GenesService} from '@features/genes/genes.service';
import {isPlatformBrowser} from '@angular/common';

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
  private geneService = inject(GenesService);
  private notify = inject(NotificationService);
  protected isExportinProgress = signal<boolean>(false);
  private readonly platformId = inject(PLATFORM_ID);

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      this.store.searchGene(this.store.activeFilters() ?? {});
    }
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
    void this.router.navigate(['/genes', row.accession]);
  }

  protected exportResultCsv() {
    const _result = this.store.searchResult();
    if (!_result) {
      this.notify.error('No data to export.');
    } else {
      const _filters = this.store.activeFilters();
      if (_filters) {
        this.isExportinProgress.set(true);
        this.geneService.exportCsv({
          ..._filters,
          page: 0,
          size: 1,
          direction: 'asc'
        }).subscribe({
          next: (blob: Blob) => {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.style.display = 'none';
            a.href = url;
            a.download = `proteins_${new Date().toISOString().split('T')[0]}.csv`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
            this.isExportinProgress.set(false);
            this.notify.success('Search result was exported successfully');
          },
          error: err => {
            const message = err.error?.message || 'Export failed due to an error';
            this.isExportinProgress.set(false);
            this.notify.error(message);
          }
        });
      } else {
        this.notify.error('No active filters to export.');
      }
    }

  }
}

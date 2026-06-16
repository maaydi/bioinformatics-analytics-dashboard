import {ChangeDetectionStrategy, Component, signal, viewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatGridListModule} from '@angular/material/grid-list';
import {MatCardModule} from '@angular/material/card';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatButtonModule} from '@angular/material/button';
import {GeneFilterComponent} from '@features/genes/gene-filter/gene-filter.component';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {PagedResponse} from '@core/models/paged-response.model';
import {ProteinSummary} from '@core/models/protein.model';

/**
 * Compare Component
 *
 * Enables side-by-side comparison of two gene filter queries and their results.
 *
 * Layout:
 * - Row 1: Two filter panels (Filter A | Filter B) at 50% width each
 * - Row 2: Two analytics dashboard sections below each filter
 *
 * State:
 * - filterA, filterB: Current filter snapshots
 * - resultsA, resultsB: Paginated search results for each filter
 * - loadingA, loadingB: Loading states for async operations
 */
@Component({
  selector: 'app-compare',
  standalone: true,
  imports: [
    CommonModule,
    MatGridListModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    GeneFilterComponent,
  ],
  templateUrl: './compare.component.html',
  styleUrl: './compare.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CompareComponent {

  readonly filterAComp = viewChild.required(GeneFilterComponent);
  readonly filterBComp = viewChild.required(GeneFilterComponent);

  // Filter state: Current filter snapshots for A and B
  readonly filterA = signal<GeneFilterSnapshot | null>(null);
  readonly filterB = signal<GeneFilterSnapshot | null>(null);

  // Results state: Paginated protein search results
  readonly resultsA = signal<PagedResponse<ProteinSummary> | null>(null);
  readonly resultsB = signal<PagedResponse<ProteinSummary> | null>(null);

  // Loading state
  readonly loadingA = signal<boolean>(false);
  readonly loadingB = signal<boolean>(false);

  // Error state
  readonly errorA = signal<string | null>(null);
  readonly errorB = signal<string | null>(null);

  get isValid(): boolean {
    return this.filterAComp().isValid && this.filterBComp().isValid;
  }

  triggerCompare(): void {
    this.filterAComp().submitForm();
    this.filterBComp().submitForm();
  }

  reset(): void {
    console.log('reset filters TODO');
  }

  /**
   * Applies Filter A: updates filterA signal and triggers search.
   */
  applyFilterA(snapshot: GeneFilterSnapshot): void {
    console.log('Filter A ');
    console.log(snapshot);
    this.filterA.set(snapshot);
    this.searchA(snapshot);
  }

  /**
   * Applies Filter B: updates filterB signal and triggers search.
   */
  applyFilterB(snapshot: GeneFilterSnapshot): void {
    console.log('Filter A ');
    console.log(snapshot);
    this.filterB.set(snapshot);
    this.searchB(snapshot);
  }

  /**
   * Clears Filter A and resets its state.
   */
  clearFilterA(): void {
    this.filterA.set(null);
    this.resultsA.set(null);
    this.errorA.set(null);
    this.loadingA.set(false);
  }

  /**
   * Clears Filter B and resets its state.
   */
  clearFilterB(): void {
    this.filterB.set(null);
    this.resultsB.set(null);
    this.errorB.set(null);
    this.loadingB.set(false);
  }

  /**
   * Private: Executes search for Filter A.
   */
  private searchA(snapshot: GeneFilterSnapshot): void {
    this.loadingA.set(true);
    this.errorA.set(null);

    // this.geneService.searchGenes({...snapshot, page: 0, size: 20}).subscribe({
    //   next: (result) => {
    //     this.resultsA.set(result);
    //     this.loadingA.set(false);
    //   },
    //   error: (err) => {
    //     this.errorA.set('Failed to load results for Filter A');
    //     this.loadingA.set(false);
    //     console.error('Search A error:', err);
    //   }
    // });
  }

  /**
   * Private: Executes search for Filter B.
   */
  private searchB(snapshot: GeneFilterSnapshot): void {
    this.loadingB.set(true);
    this.errorB.set(null);

    // this.geneService.searchGenes({...snapshot, page: 0, size: 20}).subscribe({
    //   next: (result) => {
    //     this.resultsB.set(result);
    //     this.loadingB.set(false);
    //   },
    //   error: (err) => {
    //     this.errorB.set('Failed to load results for Filter B');
    //     this.loadingB.set(false);
    //     console.error('Search B error:', err);
    //   }
    // });
  }


}

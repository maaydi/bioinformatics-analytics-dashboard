import {ChangeDetectionStrategy, Component, inject, signal, viewChild} from '@angular/core';
import {CommonModule} from '@angular/common';
import {MatGridListModule} from '@angular/material/grid-list';
import {MatCardModule} from '@angular/material/card';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatButtonModule} from '@angular/material/button';
import {GeneFilterComponent} from '@features/genes/gene-filter/gene-filter.component';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {AnalyticsService} from '@features/analytics/analytics.service';
import {AnalyticsSubset} from '@core/models/analytics.model';
import {
  DashboardKpiCardComponent,
  DashboardKpiViewModel
} from '@shared/components/analytics/dashboard-kpi-card/dashboard-kpi-card.component';
import {
  CompareLengthHistogramComponent
} from '@shared/components/analytics/compare-length-histogram/compare-length-histogram.component';
import {
  CompareEvidenceLevelComponent
} from '@shared/components/analytics/compare-evidence-level/compare-evidence-level.component';

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
    DashboardKpiCardComponent,
    CompareLengthHistogramComponent,
    CompareEvidenceLevelComponent,
  ],
  templateUrl: './compare.component.html',
  styleUrl: './compare.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CompareComponent {

  readonly filterAComp = viewChild.required<GeneFilterComponent>('filterAComp');
  readonly filterBComp = viewChild.required<GeneFilterComponent>('filterBComp');

  // Filter state: Current filter snapshots for A and B
  readonly filterA = signal<GeneFilterSnapshot | null>(null);
  readonly filterB = signal<GeneFilterSnapshot | null>(null);

  // Results state: Paginated protein search results
  readonly resultsA = signal<AnalyticsSubset | null>(null);
  readonly resultsB = signal<AnalyticsSubset | null>(null);

  // Loading state
  readonly loadingA = signal<boolean>(false);
  readonly loadingB = signal<boolean>(false);

  // Error state
  readonly errorA = signal<string | null>(null);
  readonly errorB = signal<string | null>(null);

  // KPI cards
  protected readonly kpiCardsA = signal<ReadonlyArray<DashboardKpiViewModel>>([]);
  protected readonly kpiCardsB = signal<ReadonlyArray<DashboardKpiViewModel>>([]);

  private readonly service = inject(AnalyticsService);
  private readonly numberFormatter = new Intl.NumberFormat('en-US');

  get isValid(): boolean {
    return this.filterAComp().isValid && this.filterBComp().isValid;
  }

  triggerCompare(): void {
    this.filterAComp().submitForm();
    this.filterBComp().submitForm();
    this.search(this.filterA()!, this.filterB()!);
  }

  reset(): void {
    this.clearFilterA();
    this.clearFilterB();
  }


  /**
   * Applies Filter A: updates filterA signal and triggers search.
   */
  applyFilterA(snapshot: GeneFilterSnapshot): void {
    this.filterA.set(snapshot);
  }

  /**
   * Applies Filter B: updates filterB signal and triggers search.
   */
  applyFilterB(snapshot: GeneFilterSnapshot): void {
    this.filterB.set(snapshot);
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
  private search(filterA: GeneFilterSnapshot, filterB: GeneFilterSnapshot): void {
    this.loadingA.set(true);
    this.loadingB.set(true);
    this.errorA.set(null);

    this.service.compare({
      setA: {
        ...filterA,
        page: 0,
        size: 1,
        direction: 'asc'
      }, setB: {
        ...filterB,
        page: 0,
        size: 1,
        direction: 'asc'
      }
    }).subscribe({
      next: (result) => {
        this.resultsA.set(result.subsetA);
        this.resultsB.set(result.subsetB);
        this.kpiCardsA.set(this.toKpiCards(result.subsetA));
        this.kpiCardsB.set(this.toKpiCards(result.subsetB));
        this.loadingA.set(false);
        this.loadingB.set(false);
      },
      error: (_) => {
        this.errorA.set('Failed to load results for Filter A');
        this.errorB.set('Failed to load results for Filter B');
        this.loadingA.set(false);
        this.loadingB.set(false);
      }
    });
  }

  private toKpiCards(result: AnalyticsSubset): ReadonlyArray<DashboardKpiViewModel> {
    return [
      {title: 'Total', label: 'Total proteins', value: this.numberFormatter.format(result.count)},
      {
        title: 'Avg Len',
        label: 'Average Length',
        value: this.numberFormatter.format(Math.round(result.avgLength)),
        unit: 'AA'
      },
      {title: 'Reviewed', label: 'Reviewed count', value: this.numberFormatter.format(result.reviewedCount)},
      {title: 'Ratio', label: 'Reviewed Ratio', value: this.numberFormatter.format(result.reviewedRatio)},
    ];
  }


}

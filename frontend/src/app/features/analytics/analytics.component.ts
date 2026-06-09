import {ChangeDetectionStrategy, Component, inject, OnInit, signal} from '@angular/core';
import {
  DashboardKpiCardListComponent
} from '@shared/components/analytics/dashboard-kpi-card-list/dashboard-kpi-card-list.component';
import {
  DashboardLengthHistogramComponent
} from '@shared/components/analytics/dashboard-length-histogram/dashboard-length-histogram.component';
import {
  DashboardReviewedRatioComponent
} from '@shared/components/analytics/dashboard-reviewed-ratio/dashboard-reviewed-ratio.component';
import {
  DashboardTopOrganismsComponent
} from '@shared/components/analytics/dashboard-top-organisms/dashboard-top-organisms.component';
import {AnalyticsProvider} from '@shared/components/analytics/analytics-provider';
import {AnalyticsService} from '@features/analytics/analytics.service';
import {
  DashboardEvidenceLevelsPieChartComponent
} from '@shared/components/analytics/dashboard-evidence-levels-pie-chart/dashboard-evidence-levels-pie-chart.component';
import {
  DashboardKeywordFrequencyHistogramComponent
} from '@shared/components/analytics/dashboard-keyword-frequency-histogram/dashboard-keyword-frequency-histogram.component';
import {SavedFilter} from '@core/models/saved-filter.model';
import {MatIcon} from '@angular/material/icon';
import {SavedFiltersService} from '@features/saved-filters/saved-filters.service';
import {MatCard} from '@angular/material/card';
import {LoadingSpinnerComponent} from '@shared/components/loading-spinner/loading-spinner.component';

/**
 * Analytics page — Epic 4 full view.
 *
 * Interactive charts:
 * - Protein length histogram (US-11) OK
 * - Evidence level pie chart (US-12) OK
 * - Chart-to-table drill-down (US-13)
 * - Dual-subset comparison (US-14)
 * - Proteins by organism bar chart
 * - Reviewed/unreviewed ratio OK
 * - Keyword frequency chart
 * - Length vs Molecular Weight scatter
 *
 */
@Component({
  selector: 'app-analytics',
  imports: [
    DashboardKpiCardListComponent,
    DashboardLengthHistogramComponent,
    DashboardReviewedRatioComponent,
    DashboardTopOrganismsComponent,
    DashboardEvidenceLevelsPieChartComponent,
    DashboardKeywordFrequencyHistogramComponent,
    MatIcon,
    MatCard,
    LoadingSpinnerComponent
  ],
  providers: [
    {provide: AnalyticsProvider, useExisting: AnalyticsService}
  ],
  templateUrl: './analytics.component.html',
  styleUrls: ['./analytics.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AnalyticsComponent implements OnInit {
  protected dashboardKpiCardLoading = signal<boolean>(true);

  /** True while loading saved filters from API. */
  protected loading = signal<boolean>(true);

  /** Array of user's saved filter sets. */
  protected filters = signal<SavedFilter[]>([]);

  protected selectedFilter = signal<SavedFilter | null>(null);

  /** User-facing error message, or null if no error. */
  protected errors = signal<string | null>(null);

  private readonly service = inject(SavedFiltersService);

  /**
   * Load saved filters on component init.
   */
  ngOnInit(): void {
    this.service.listSavedFilters().subscribe({
      next: sf => {
        this.filters.set(sf || []);
        this.errors.set(null);
        this.loading.set(false);
      },
      error: _ => {
        this.errors.set('Failed to load saved filters');
        this.loading.set(false);
      }
    });
  }

  protected onFilterSelect(event: Event): void {
    const selectElement = event.target as HTMLSelectElement;
    const filterId = Number(selectElement.value);

    if (!filterId) {
      this.selectedFilter.set(null);
      return;
    }

    const matchedFilter = this.filters().find(f => f.id === filterId);
    console.log(matchedFilter?.filterJson);
    this.selectedFilter.set(matchedFilter ?? null);

    console.log('Selected analytics filter:', this.selectedFilter());
  }
}

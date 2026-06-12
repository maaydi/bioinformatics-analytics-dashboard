import {ChangeDetectionStrategy, Component, ElementRef, inject, OnInit, signal, ViewChild} from '@angular/core';
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
import {
  DashboardScatterLengthWeightComponent
} from '@features/analytics/dashboard/dashboard-scatter-length-weight/dashboard-scatter-length-weight.component';
import {MatButton} from '@angular/material/button';
import html2canvas from 'html2canvas';
import {CompareComponent} from '@features/analytics/compare/compare.component';

/**
 * Analytics page — Epic 4 full view.
 *
 * Interactive charts:
 * - Protein length histogram (US-11) DONE
 * - Evidence level pie chart (US-12) DONE
 * - Chart-to-table drill-down (US-13) DONE
 * - Dual-subset comparison (US-14)
 * - Proteins by organism bar chart DONE
 * - Reviewed/unreviewed ratio DONE
 * - Keyword frequency chart DONE
 * - Length vs Molecular Weight scatter DONE
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
    LoadingSpinnerComponent,
    DashboardScatterLengthWeightComponent,
    MatButton,
    CompareComponent,
  ],
  providers: [{provide: AnalyticsProvider, useExisting: AnalyticsService}],
  templateUrl: './analytics.component.html',
  styleUrls: ['./analytics.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AnalyticsComponent implements OnInit {
  @ViewChild('captureArea')
  captureArea!: ElementRef;

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
      next: (sf) => {
        this.filters.set(sf || []);
        this.errors.set(null);
        this.loading.set(false);
      },
      error: (_) => {
        this.errors.set('Failed to load saved filters');
        this.loading.set(false);
      },
    });
  }

  protected onFilterSelect(event: Event): void {
    const selectElement = event.target as HTMLSelectElement;
    const filterId = Number(selectElement.value);

    if (!filterId) {
      this.selectedFilter.set(null);
      return;
    }

    const matchedFilter = this.filters().find((f) => f.id === filterId);
    this.selectedFilter.set(matchedFilter ?? null);
  }

  protected async exportDashboardAsImage() {
    const canvas = await html2canvas(this.captureArea.nativeElement, {
      scale: 2,
      backgroundColor: '#ffffff',
    });

    const image = canvas.toDataURL('image/png');

    const link = document.createElement('a');
    link.href = image;
    link.download = `analytics-dashboard-${this.selectedFilter()?.name.replaceAll(' ', '_')}-${new Date().toISOString()}.png`;
    link.click();
  }
}

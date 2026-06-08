import {ChangeDetectionStrategy, Component, signal} from '@angular/core';
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
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';

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
    DashboardKeywordFrequencyHistogramComponent
  ],
  providers: [
    {provide: AnalyticsProvider, useExisting: AnalyticsService}
  ],
  templateUrl: './analytics.component.html',
  styleUrl: './analytics.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AnalyticsComponent {
  protected dashboardKpiCardLoading = signal<boolean>(true);

  // TODO add component to choose filter from saved filters and fix backend endpoints
  protected readonly filters: GeneFilterSnapshot = {reviewed: true};
}

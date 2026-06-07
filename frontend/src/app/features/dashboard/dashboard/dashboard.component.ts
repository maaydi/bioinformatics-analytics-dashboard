import {ChangeDetectionStrategy, Component, signal} from '@angular/core';
import {
  DashboardLengthHistogramComponent
} from '@shared/components/analytics/dashboard-length-histogram/dashboard-length-histogram.component';
import {
  DashboardReviewedRatioComponent
} from '@shared/components/analytics/dashboard-reviewed-ratio/dashboard-reviewed-ratio.component';
import {
  DashboardEvidenceLevelsComponent
} from '@shared/components/analytics/dashboard-evidence-levels/dashboard-evidence-levels.component';
import {
  DashboardTopOrganismsComponent
} from '@shared/components/analytics/dashboard-top-organisms/dashboard-top-organisms.component';
import {DashboardService} from '@features/dashboard/dashboard.service';
import {AnalyticsProvider} from '@shared/components/analytics/analytics-provider';
import {
  DashboardKpiCardListComponent
} from '@shared/components/analytics/dashboard-kpi-card-list/dashboard-kpi-card-list.component';

/**
 * Dashboard layout container for DASH-001.
 *
 * This component orchestrates the visual grid and delegates each visualization
 * to a dedicated presentational component.
 */
@Component({
  selector: 'app-dashboard',
  imports: [
    DashboardLengthHistogramComponent,
    DashboardReviewedRatioComponent,
    DashboardEvidenceLevelsComponent,
    DashboardTopOrganismsComponent,
    DashboardKpiCardListComponent,
  ],
  providers: [
    {provide: AnalyticsProvider, useExisting: DashboardService}
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})

export class DashboardComponent {

  protected readonly dashboardKpiCardLoading = signal<boolean>(true);
}


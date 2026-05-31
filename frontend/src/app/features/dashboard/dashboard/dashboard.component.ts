import {ChangeDetectionStrategy, Component, signal} from '@angular/core';
import {
  DashboardKpiCardComponent
} from '@features/dashboard/components/dashboard-kpi-card/dashboard-kpi-card.component';
import {
  DashboardLengthHistogramComponent
} from '@features/dashboard/components/dashboard-length-histogram/dashboard-length-histogram.component';
import {
  DashboardReviewedRatioComponent
} from '@features/dashboard/components/dashboard-reviewed-ratio/dashboard-reviewed-ratio.component';
import {
  DashboardEvidenceLevelsComponent
} from '@features/dashboard/components/dashboard-evidence-levels/dashboard-evidence-levels.component';
import {
  DashboardTopOrganismsComponent
} from '@features/dashboard/components/dashboard-top-organisms/dashboard-top-organisms.component';

/**
 * Dashboard layout container for DASH-001.
 *
 * This component orchestrates the visual grid and delegates each visualization
 * to a dedicated presentational component.
 */
@Component({
  selector: 'app-dashboard',
  imports: [
    DashboardKpiCardComponent,
    DashboardLengthHistogramComponent,
    DashboardReviewedRatioComponent,
    DashboardEvidenceLevelsComponent,
    DashboardTopOrganismsComponent,
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent {
  protected readonly kpiCards = signal<ReadonlyArray<DashboardKpiViewModel>>([
    {title: 'Total', label: 'Total proteins', value: '570,122'},
    {title: 'Reviewed', label: 'Reviewed count', value: '312,048'},
    {title: 'Organisms', label: 'Distinct organisms', value: '14,293'},
    {title: 'Avg Len', label: 'Average Length', value: '387', unit: 'AA'},
    {title: 'Top Org', label: 'Top Organism', value: 'Homo sapiens'},
  ]);
}

interface DashboardKpiViewModel {
  readonly title: string;
  readonly label: string;
  readonly value: string;
  readonly unit?: string;
}

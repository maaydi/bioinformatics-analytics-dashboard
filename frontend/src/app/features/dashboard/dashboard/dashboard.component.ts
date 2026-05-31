import {ChangeDetectionStrategy, Component, DestroyRef, inject, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {HttpErrorResponse} from '@angular/common/http';
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
import {DashboardService} from '@features/dashboard/dashboard.service';
import {DashboardKpis} from '@core/models/analytics.model';

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
  protected readonly kpiCards = signal<ReadonlyArray<DashboardKpiViewModel>>([]);
  protected readonly kpiLoading = signal<boolean>(true);
  protected readonly kpiError = signal<string | null>(null);

  private readonly dashboardService = inject(DashboardService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly numberFormatter = new Intl.NumberFormat('en-US');

  constructor() {
    this.loadKpis();
  }

  protected retryKpis(): void {
    this.loadKpis();
  }


  private loadKpis(): void {
    this.kpiLoading.set(true);
    this.kpiError.set(null);

    this.dashboardService.getDashboardKpis()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (kpis) => {
          this.kpiCards.set(this.toKpiCards(kpis));
          this.kpiLoading.set(false);
        },
        error: (_error: HttpErrorResponse) => {
          this.kpiCards.set([]);
          this.kpiError.set('Unable to load dashboard KPIs.');
          this.kpiLoading.set(false);
        }
      });
  }

  private toKpiCards(kpis: DashboardKpis): ReadonlyArray<DashboardKpiViewModel> {
    return [
      {title: 'Total', label: 'Total proteins', value: this.numberFormatter.format(kpis.totalProteins)},
      {title: 'Reviewed', label: 'Reviewed count', value: this.numberFormatter.format(kpis.reviewedCount)},
      {title: 'Organisms', label: 'Distinct organisms', value: this.numberFormatter.format(kpis.organismCount)},
      {title: 'Taxa', label: 'Distinct taxa', value: this.numberFormatter.format(kpis.taxonCount)},
      {
        title: 'Avg Len',
        label: 'Average Length',
        value: this.numberFormatter.format(Math.round(kpis.avgLength)),
        unit: 'AA'
      },
    ];
  }
}

interface DashboardKpiViewModel {
  readonly title: string;
  readonly label: string;
  readonly value: string;
  readonly unit?: string;
}

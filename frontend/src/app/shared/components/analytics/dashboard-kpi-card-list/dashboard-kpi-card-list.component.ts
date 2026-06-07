import {ChangeDetectionStrategy, Component, DestroyRef, inject, model, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {HttpErrorResponse} from '@angular/common/http';
import {DashboardKpiCardComponent} from '@shared/components/analytics/dashboard-kpi-card/dashboard-kpi-card.component';
import {DashboardKpis} from '@core/models/analytics.model';
import {AnalyticsProvider} from '@shared/components/analytics/analytics-provider';

interface DashboardKpiViewModel {
  readonly title: string;
  readonly label: string;
  readonly value: string;
  readonly unit?: string;
}

/**
 * Dashboard KPI Card list
 *
 * This component orchestrates the visual grid and delegates each visualization
 * to a dedicated presentational component.
 */
@Component({
  selector: 'app-dashboard-kpi-card-list',
  imports: [
    DashboardKpiCardComponent
  ],
  templateUrl: './dashboard-kpi-card-list.component.html',
  styleUrl: './dashboard-kpi-card-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})

export class DashboardKpiCardListComponent {
  public readonly kpiLoading = model<boolean>(true);
  protected readonly kpiCards = signal<ReadonlyArray<DashboardKpiViewModel>>([]);
  protected readonly kpiError = signal<string | null>(null);

  private readonly analyticProvider = inject(AnalyticsProvider);
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

    this.analyticProvider.getDashboardKpis()
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



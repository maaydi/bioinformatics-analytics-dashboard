import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  inject,
  input,
  model,
  PLATFORM_ID,
  signal
} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {HttpErrorResponse} from '@angular/common/http';
import {
  DashboardKpiCardComponent,
  DashboardKpiViewModel
} from '@shared/components/analytics/dashboard-kpi-card/dashboard-kpi-card.component';
import {DashboardKpis} from '@core/models/analytics.model';
import {AnalyticsProvider} from '@shared/components/analytics/analytics-provider';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {Subscription} from 'rxjs';
import {MatButton} from '@angular/material/button';
import {isPlatformBrowser} from '@angular/common';


/**
 * Dashboard KPI Card list
 *
 * This component orchestrates the visual grid and delegates each visualization
 * to a dedicated presentational component.
 */
@Component({
  selector: 'app-dashboard-kpi-card-list',
  imports: [
    DashboardKpiCardComponent,
    MatButton
  ],
  templateUrl: './dashboard-kpi-card-list.component.html',
  styleUrl: './dashboard-kpi-card-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})

export class DashboardKpiCardListComponent {
  public readonly filter = input<GeneFilterSnapshot | undefined>(undefined);
  public readonly kpiLoading = model<boolean>(true);
  protected readonly kpiCards = signal<ReadonlyArray<DashboardKpiViewModel>>([]);
  protected readonly kpiError = signal<string | null>(null);

  private readonly analyticProvider = inject(AnalyticsProvider);
  private readonly destroyRef = inject(DestroyRef);
  private readonly numberFormatter = new Intl.NumberFormat('en-US');

  private kpiSub?: Subscription;

  private readonly platformId = inject(PLATFORM_ID);

  constructor() {
    effect(() => {
      if (isPlatformBrowser(this.platformId)) {
        this.loadKpis();
      }
    });
  }

  protected retryKpis(): void {
    this.loadKpis();
  }


  private loadKpis(): void {
    this.kpiSub?.unsubscribe();
    this.kpiLoading.set(true);
    this.kpiError.set(null);

    this.kpiSub = this.analyticProvider.getDashboardKpis(this.filter())
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



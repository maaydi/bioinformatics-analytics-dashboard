import {HttpErrorResponse} from '@angular/common/http';
import {ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {ReviewedRatioItem} from '@core/models/analytics.model';
import {LoadingSpinnerComponent} from '@shared/components/loading-spinner/loading-spinner.component';
import {Router} from '@angular/router';
import {GenesStore} from '@features/genes/state/filters.store';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {AnalyticsProvider} from '@shared/components/analytics/analytics-provider';

@Component({
  selector: 'app-dashboard-reviewed-ratio',
  imports: [MatCardModule, LoadingSpinnerComponent, MatButtonModule],
  templateUrl: './dashboard-reviewed-ratio.component.html',
  styleUrl: './dashboard-reviewed-ratio.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardReviewedRatioComponent {
  protected readonly loading = signal<boolean>(true);
  protected readonly error = signal<string | null>(null);
  protected readonly totalCount = computed<number>(() => this.reviewedCount() + this.unreviewedCount());
  protected readonly hasData = computed<boolean>(() => this.totalCount() > 0);
  protected readonly reviewedPercent = computed<number>(() => {
    const total = this.totalCount();
    if (total === 0) {
      return 0;
    }

    return Math.round((this.reviewedCount() / total) * 100);
  });
  protected readonly unreviewedPercent = computed<number>(() => 100 - this.reviewedPercent());
  protected readonly donutBackground = computed<string>(() => {
    const reviewed = this.reviewedPercent();
    return `conic-gradient(#1565c0 0 ${reviewed}%, #93c5fd ${reviewed}% 100%)`;
  });
  private readonly data = signal<ReadonlyArray<ReviewedRatioItem>>([]);
  protected readonly reviewedCount = computed<number>(() =>
    this.data().find((item) => item.reviewed)?.count ?? 0
  );
  protected readonly unreviewedCount = computed<number>(() =>
    this.data().find((item) => !item.reviewed)?.count ?? 0
  );
  private readonly analyticProvider = inject(AnalyticsProvider);
  private readonly destroyRef = inject(DestroyRef);
  private readonly genesStore = inject(GenesStore);
  private readonly router = inject(Router);
  private readonly numberFormatter = new Intl.NumberFormat('en-US');

  constructor() {
    this.loadReviewedRatio();
  }

  protected retry(): void {
    this.loadReviewedRatio();
  }

  protected formatCount(count: number): string {
    return this.numberFormatter.format(count);
  }

  protected selectReviewedStatus(reviewed: boolean): void {
    const snapshot: GeneFilterSnapshot = {reviewed};
    this.genesStore.setActiveFilters(snapshot);
    void this.router.navigate(['/genes']);
  }

  private loadReviewedRatio(): void {
    this.loading.set(true);
    this.error.set(null);

    this.analyticProvider.getReviewedRatio()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.data.set(response);
          this.loading.set(false);
        },
        error: (_error: HttpErrorResponse) => {
          this.data.set([]);
          this.error.set('Unable to load reviewed ratio data.');
          this.loading.set(false);
        }
      });
  }
}


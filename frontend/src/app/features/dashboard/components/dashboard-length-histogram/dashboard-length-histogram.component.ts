import {DecimalPipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';
import {ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {MatCardModule} from '@angular/material/card';
import {DashboardService} from '@features/dashboard/dashboard.service';
import {LengthHistogramBucket} from '@core/models/analytics.model';

interface HistogramBucket {
  readonly rangeLabel: string;  // e.g. "0–100"
  readonly count: number;       // protein count in that bucket
}

@Component({
  selector: 'app-dashboard-length-histogram',
  imports: [MatCardModule, DecimalPipe],
  templateUrl: './dashboard-length-histogram.component.html',
  styleUrl: './dashboard-length-histogram.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardLengthHistogramComponent {
  protected readonly loading = signal<boolean>(true);
  protected readonly error = signal<string | null>(null);
  /** 5 evenly-spaced Y-axis tick labels from 0 to maxCount */
  protected readonly yTicks = computed<ReadonlyArray<number>>(() => {
    const max = this.maxCount();
    if (max <= 0) {
      return [0, 0, 0, 0, 0];
    }

    const step = Math.ceil(max / 4);
    return [step * 4, step * 3, step * 2, step, 0];
  });
  private readonly buckets = signal<ReadonlyArray<LengthHistogramBucket>>([]);
  protected readonly hasData = computed<boolean>(() => this.buckets().length > 0);
  protected readonly barGridTemplate = computed<string>(() => {
    const bucketCount = this.buckets().length;
    return `repeat(${Math.max(bucketCount, 1)}, minmax(0, 1fr))`;
  });
  protected readonly viewBuckets = computed<ReadonlyArray<HistogramBucket>>(() => {
    return this.buckets().map((bucket) => ({
      rangeLabel: `${bucket.rangeMin}-${bucket.rangeMax}`,
      count: bucket.count,
    }));
  });
  protected readonly maxCount = computed(() =>
    Math.max(...this.buckets().map((bucket) => bucket.count), 0)
  );
  private readonly dashboardService = inject(DashboardService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.loadHistogram();
  }

  /** Height % of each bar relative to maxCount */
  protected barHeight(count: number): number {
    const max = this.maxCount();
    return max === 0 ? 0 : Math.round((count / max) * 100);
  }

  private loadHistogram(): void {
    this.loading.set(true);
    this.error.set(null);

    this.dashboardService.getLengthHistogram()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.buckets.set(response);
          this.loading.set(false);
        },
        error: (_error: HttpErrorResponse) => {
          this.buckets.set([]);
          this.error.set('Unable to load protein length distribution.');
          this.loading.set(false);
        }
      });
  }
}

import {DecimalPipe} from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  input,
  signal,
  ViewChild
} from '@angular/core';
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {LoadingSpinnerComponent} from '@shared/components/loading-spinner/loading-spinner.component';
import {Router} from '@angular/router';
import {LimitSelectorComponent} from '@shared/components/limit-selector/limit-selector.component';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {HttpErrorResponse} from '@angular/common/http';
import {AnalyticsProvider} from '@shared/components/analytics/analytics-provider';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {GenesStore} from '@features/genes/state/filters.store';
import {Subscription} from 'rxjs';
import {MatIcon} from '@angular/material/icon';
import {ImageExportService} from '@shared/directive/image-export-service';

interface KeywordBucket {
  readonly keyword: string;
  readonly count: number;
}

@Component({
  selector: 'app-dashboard-keyword-frequency-histogram',
  imports: [MatCardModule, DecimalPipe, LoadingSpinnerComponent, MatButtonModule, LimitSelectorComponent, MatIcon],
  templateUrl: './dashboard-keyword-frequency-histogram.component.html',
  styleUrl: './dashboard-keyword-frequency-histogram.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardKeywordFrequencyHistogramComponent {
  public readonly filter = input<GeneFilterSnapshot | undefined>(undefined);
  private keywordSub?: Subscription;
  protected readonly Math = Math;
  protected readonly loading = signal<boolean>(true);
  protected readonly error = signal<string | null>(null);
  protected readonly buckets = signal<ReadonlyArray<KeywordBucket>>([]);
  protected readonly hasData = computed<boolean>(() => this.buckets().length > 0);
  protected readonly barGridTemplate = computed<string>(() => {
    const bucketCount = this.visibleBuckets().length;
    return `repeat(${Math.max(bucketCount, 1)}, minmax(0, 1fr))`;
  });
  protected readonly totalCount = computed<number>(() =>
    this.visibleBuckets().reduce((total, b) => total + b.count, 0)
  );
  protected readonly maxCount = computed(() =>
    Math.max(...this.visibleBuckets().map((b) => b.count), 0)
  );
  private readonly _limit = signal<number>(100);
  public readonly limit = this._limit.asReadonly();
  protected readonly visibleBuckets = computed<ReadonlyArray<KeywordBucket>>(() =>
    this.buckets().slice(0, Math.max(0, this._limit()))
  );
  private readonly genesStore = inject(GenesStore);
  private readonly analyticProvider = inject(AnalyticsProvider);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);

  @ViewChild('chartCard', {read: ElementRef})
  chartCard!: ElementRef<HTMLElement>;

  private readonly imageExportService = inject(ImageExportService);

  constructor() {
    effect(() => {
      this.loadKeywordFrequency();
    });
  }

  protected retry(): void {
    this.loadKeywordFrequency();
  }

  protected barHeight(count: number): number {
    const max = this.maxCount();
    return max === 0 ? 0 : Math.round((count / max) * 100);
  }

  protected barShare(count: number): number {
    const total = this.totalCount();
    return total === 0 ? 0 : (count / total) * 100;
  }

  protected selectKeyword(bucket: KeywordBucket) {
    const snapshot: GeneFilterSnapshot = {keywords: [bucket.keyword]};
    this.genesStore.setActiveFilters(snapshot);
    void this.router.navigate(['/genes']);
  }

  protected onLimitChanged(newLimit: number): void {
    if (this._limit() === newLimit) return;
    this._limit.set(newLimit);
    this.loadKeywordFrequency();
  }

  private loadKeywordFrequency(): void {
    this.keywordSub?.unsubscribe();
    this.loading.set(true);
    this.error.set(null);

    this.keywordSub = this.analyticProvider.getKeywordFrequency(this._limit(), this.filter())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.buckets.set(response);
          this.loading.set(false);
        },
        error: (_error: HttpErrorResponse) => {
          this.buckets.set([]);
          this.error.set('Unable to load keyword frequency.');
          this.loading.set(false);
        }
      });
  }

  protected async exportAsImage(): Promise<void> {
    if (!this.chartCard) return;

    if (!this.chartCard) return;

    await this.imageExportService.exportElement(
      this.chartCard.nativeElement,
      'keyword-frequency-dashboard.png',
      '.no-export'
    );
  }
}


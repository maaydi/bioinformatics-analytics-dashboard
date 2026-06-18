import {DecimalPipe} from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  input,
  output,
  ViewChild
} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatIcon} from '@angular/material/icon';
import {LengthHistogramBucket} from '@core/models/analytics.model';
import {ImageExportService} from '@shared/directive/image-export-service';

export interface CompareHistogramBucket {
  readonly rangeLabel: string;
  readonly rangeMin: number;
  readonly rangeMax: number;
  countA: number;
  countB: number;
}

interface HistogramXAxisTick {
  readonly value: number;
  readonly align: 'start' | 'center' | 'end';
}

@Component({
  selector: 'app-compare-length-histogram',
  standalone: true,
  imports: [MatCardModule, DecimalPipe, MatButtonModule, MatIcon],
  templateUrl: './compare-length-histogram.component.html',
  styleUrl: './compare-length-histogram.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CompareLengthHistogramComponent {
  @ViewChild('chartCard', {read: ElementRef})
  chartCard!: ElementRef<HTMLElement>;
  // Inputs
  public readonly bucketsA = input.required<ReadonlyArray<LengthHistogramBucket>>();
  public readonly bucketsB = input.required<ReadonlyArray<LengthHistogramBucket>>();
  public readonly labelA = input<string>('Set A');
  public readonly labelB = input<string>('Set B');
  // Output for clicking a bar (optional, keeps component decoupled)
  public readonly rangeSelected = output<{ min: number; max: number }>();
  // Unify the buckets by range so X-axis aligns properly
  protected readonly viewBuckets = computed<ReadonlyArray<CompareHistogramBucket>>(() => {
    const a = this.bucketsA() || [];
    const b = this.bucketsB() || [];
    const map = new Map<number, CompareHistogramBucket>();

    const process = (buckets: ReadonlyArray<LengthHistogramBucket>, isA: boolean) => {
      for (const bk of buckets) {
        if (!map.has(bk.rangeMin)) {
          map.set(bk.rangeMin, {
            rangeLabel: `${bk.rangeMin}-${bk.rangeMax}`,
            rangeMin: bk.rangeMin,
            rangeMax: bk.rangeMax,
            countA: 0,
            countB: 0
          });
        }
        const merged = map.get(bk.rangeMin)!;
        if (isA) merged.countA = bk.count;
        else merged.countB = bk.count;
      }
    };

    process(a, true);
    process(b, false);

    // Sort by rangeMin to guarantee correct X-axis order
    return Array.from(map.values()).sort((x, y) => x.rangeMin - y.rangeMin);
  });
  protected readonly hasData = computed<boolean>(() => this.viewBuckets().length > 0);
  protected readonly totalCountA = computed<number>(() =>
    this.viewBuckets().reduce((total, bucket) => total + bucket.countA, 0)
  );
  protected readonly totalCountB = computed<number>(() =>
    this.viewBuckets().reduce((total, bucket) => total + bucket.countB, 0)
  );
  protected readonly maxCount = computed(() => {
    const maxA = Math.max(...this.viewBuckets().map(b => b.countA), 0);
    const maxB = Math.max(...this.viewBuckets().map(b => b.countB), 0);
    return Math.max(maxA, maxB);
  });
  protected readonly yTicks = computed<ReadonlyArray<number>>(() => {
    const max = this.maxCount();
    if (max <= 0) return [0, 0, 0, 0, 0];
    const step = Math.ceil(max / 4);
    return [step * 4, step * 3, step * 2, step, 0];
  });
  protected readonly xAxisTicks = computed<ReadonlyArray<HistogramXAxisTick>>(() => {
    const buckets = this.viewBuckets();
    if (buckets.length === 0) return [];
    const min = buckets[0].rangeMin;
    const max = buckets[buckets.length - 1].rangeMax;
    const median = Math.round((min + max) / 2);

    return [
      {value: min, align: 'start'},
      {value: median, align: 'center'},
      {value: max, align: 'end'},
    ];
  });
  protected readonly barGridTemplate = computed<string>(() => {
    const bucketCount = this.viewBuckets().length;
    return `repeat(${Math.max(bucketCount, 1)}, minmax(0, 1fr))`;
  });
  private readonly imageExportService = inject(ImageExportService);

  protected barHeight(count: number): number {
    const max = this.maxCount();
    return max === 0 ? 0 : Math.round((count / max) * 100);
  }

  protected barShareA(count: number): number {
    const total = this.totalCountA();
    return total === 0 ? 0 : (count / total) * 100;
  }

  protected barShareB(count: number): number {
    const total = this.totalCountB();
    return total === 0 ? 0 : (count / total) * 100;
  }

  protected selectLengthRange(bucket: CompareHistogramBucket): void {
    this.rangeSelected.emit({min: bucket.rangeMin, max: bucket.rangeMax});
  }

  protected async exportAsImage(): Promise<void> {
    if (!this.chartCard) return;
    await this.imageExportService.exportElement(
      this.chartCard.nativeElement,
      'compare-length-histogram',
      '.no-export'
    );
  }
}

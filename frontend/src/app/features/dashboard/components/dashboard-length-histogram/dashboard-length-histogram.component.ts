import {DecimalPipe} from '@angular/common';
import {ChangeDetectionStrategy, Component, computed, signal} from '@angular/core';
import {MatCardModule} from '@angular/material/card';

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

  // Placeholder data — will be replaced by real API data in DASH-001 service wiring
  protected readonly buckets = signal<ReadonlyArray<HistogramBucket>>([
    {rangeLabel: '0–100', count: 18400},
    {rangeLabel: '100–200', count: 32700},
    {rangeLabel: '200–300', count: 41500},
    {rangeLabel: '300–400', count: 52800},
    {rangeLabel: '400–500', count: 46200},
    {rangeLabel: '500–600', count: 34100},
    {rangeLabel: '600–700', count: 24300},
    {rangeLabel: '700+', count: 15600},
  ]);

  protected readonly maxCount = computed(() =>
    Math.max(...this.buckets().map(b => b.count))
  );

  /** 5 evenly-spaced Y-axis tick labels from 0 to maxCount */
  protected readonly yTicks = computed<ReadonlyArray<number>>(() => {
    const max = this.maxCount();
    const step = Math.ceil(max / 4 / 1000) * 1000;
    return [step * 4, step * 3, step * 2, step, 0];
  });

  /** Height % of each bar relative to maxCount */
  protected barHeight(count: number): number {
    const max = this.maxCount();
    return max === 0 ? 0 : Math.round((count / max) * 100);
  }
}

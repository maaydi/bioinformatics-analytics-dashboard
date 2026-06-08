import {ChangeDetectionStrategy, Component, computed} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {DecimalPipe} from '@angular/common';
import {LoadingSpinnerComponent} from '@shared/components/loading-spinner/loading-spinner.component';
import {AbstractDashboardEvidenceLevelsDirective} from '@shared/directive/dashboard-evidence-level.directive';

interface EvidenceLevelView {
  readonly level: number;
  readonly label: string;
  readonly count: number;
  readonly ratioPercent: number;
  readonly colorClass: string;
}

@Component({
  selector: 'app-dashboard-evidence-levels-pie-chart',
  imports: [MatCardModule, DecimalPipe, LoadingSpinnerComponent, MatButtonModule],
  templateUrl: './dashboard-evidence-levels-pie-chart.component.html',
  styleUrl: './dashboard-evidence-levels-pie-chart.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardEvidenceLevelsPieChartComponent extends AbstractDashboardEvidenceLevelsDirective {

  protected readonly totalCount = computed<number>(() =>
    this.evidenceItems().reduce((sum, item) => sum + item.count, 0)
  );

  protected readonly items = computed<ReadonlyArray<EvidenceLevelView>>(() => {
    const items = this.sortedItems();
    const total = this.totalCount();

    return items.map((item) => ({
      level: item.evidenceLevel,
      label: item.label,
      count: item.count,
      ratioPercent: total > 0 ? Math.round((item.count / total) * 100) : 0,
      colorClass: this.toColorClass(item.evidenceLevel),
    }));
  });

  protected readonly pieGradient = computed<string>(() => {
    const items = this.sortedItems();
    const total = this.totalCount();

    if (total === 0) return 'none';

    let accumulatedPercentage = 0;
    const segments = items.map((item) => {
      const start = accumulatedPercentage;
      const slicePercentage = (item.count / total) * 100;
      accumulatedPercentage += slicePercentage;

      const colorVar = `var(--${this.toColorClass(item.evidenceLevel)})`;
      return `${colorVar} ${start.toFixed(2)}% ${accumulatedPercentage.toFixed(2)}%`;
    });

    return `conic-gradient(${segments.join(', ')})`;
  });

}

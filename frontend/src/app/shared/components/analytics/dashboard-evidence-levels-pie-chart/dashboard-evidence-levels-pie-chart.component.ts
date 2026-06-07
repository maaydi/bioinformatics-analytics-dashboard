import {ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {DecimalPipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Router} from '@angular/router';
import {EvidenceLevelItem} from '@core/models/analytics.model';
import {EvidenceLevel} from '@core/models/protein.model';
import {GenesStore} from '@features/genes/state/filters.store';
import {LoadingSpinnerComponent} from '@shared/components/loading-spinner/loading-spinner.component';
import {AnalyticsProvider} from '@shared/components/analytics/analytics-provider';

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
export class DashboardEvidenceLevelsPieChartComponent {
  protected readonly loading = signal<boolean>(true);
  protected readonly error = signal<string | null>(null);
  private readonly evidenceItems = signal<ReadonlyArray<EvidenceLevelItem>>([]);

  protected readonly hasData = computed<boolean>(() => this.evidenceItems().length > 0);

  protected readonly totalCount = computed<number>(() =>
    this.evidenceItems().reduce((sum, item) => sum + item.count, 0)
  );

  protected readonly items = computed<ReadonlyArray<EvidenceLevelView>>(() => {
    const sortedItems = [...this.evidenceItems()]
      .sort((left, right) => left.evidenceLevel - right.evidenceLevel);
    const total = this.totalCount();

    return sortedItems.map((item) => ({
      level: item.evidenceLevel,
      label: item.label,
      count: item.count,
      ratioPercent: total > 0 ? Math.round((item.count / total) * 100) : 0,
      colorClass: this.toColorClass(item.evidenceLevel),
    }));
  });

  protected readonly pieGradient = computed<string>(() => {
    const sortedItems = [...this.evidenceItems()]
      .sort((left, right) => left.evidenceLevel - right.evidenceLevel);
    const total = this.totalCount();

    if (total === 0) return 'none';

    let accumulatedPercentage = 0;
    const segments = sortedItems.map((item) => {
      const start = accumulatedPercentage;
      const slicePercentage = (item.count / total) * 100;
      accumulatedPercentage += slicePercentage;

      const colorVar = `var(--${this.toColorClass(item.evidenceLevel)})`;
      return `${colorVar} ${start.toFixed(2)}% ${accumulatedPercentage.toFixed(2)}%`;
    });

    return `conic-gradient(${segments.join(', ')})`;
  });

  private readonly analyticProvider = inject(AnalyticsProvider);
  private readonly genesStore = inject(GenesStore);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.loadEvidenceLevels();
  }

  protected retry(): void {
    this.loadEvidenceLevels();
  }

  protected selectEvidenceLevel(evidenceLevel: number): void {
    if (!this.isEvidenceLevel(evidenceLevel)) {
      return;
    }

    this.genesStore.setActiveFilters({evidenceLevels: [evidenceLevel]});
    void this.router.navigate(['/genes']);
  }

  private loadEvidenceLevels(): void {
    this.loading.set(true);
    this.error.set(null);

    this.analyticProvider.getEvidenceLevels()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.evidenceItems.set(response);
          this.loading.set(false);
        },
        error: (_error: HttpErrorResponse) => {
          this.evidenceItems.set([]);
          this.error.set('Unable to load evidence level distribution.');
          this.loading.set(false);
        }
      });
  }

  private toColorClass(evidenceLevel: number): string {
    switch (evidenceLevel) {
      case 1:
        return 'level-l1';
      case 2:
        return 'level-l2';
      case 3:
        return 'level-l3';
      case 4:
        return 'level-l4';
      case 5:
        return 'level-l5';
      default:
        return 'level-other';
    }
  }

  private isEvidenceLevel(level: number): level is EvidenceLevel {
    return level >= 1 && level <= 5;
  }
}

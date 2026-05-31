import {ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal} from '@angular/core';
import {MatCardModule} from '@angular/material/card';
import {DecimalPipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {DashboardService} from '@features/dashboard/dashboard.service';
import {EvidenceLevelItem} from '@core/models/analytics.model';

interface EvidenceLevelView {
  readonly level: string;
  readonly label: string;
  readonly count: number;
  readonly ratioPercent: number;
  readonly colorClass: string;
}

@Component({
  selector: 'app-dashboard-evidence-levels',
  imports: [MatCardModule, DecimalPipe],
  templateUrl: './dashboard-evidence-levels.component.html',
  styleUrl: './dashboard-evidence-levels.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardEvidenceLevelsComponent {
  protected readonly loading = signal<boolean>(true);
  protected readonly error = signal<string | null>(null);
  private readonly evidenceItems = signal<ReadonlyArray<EvidenceLevelItem>>([]);

  protected readonly hasData = computed<boolean>(() => this.evidenceItems().length > 0);
  protected readonly items = computed<ReadonlyArray<EvidenceLevelView>>(() => {
    const maxCount = Math.max(...this.evidenceItems().map((item) => item.count), 0);

    return this.evidenceItems().map((item) => ({
      level: String(item.evidenceLevel),
      label: item.label,
      count: item.count,
      ratioPercent: maxCount > 0 ? Math.round((item.count / maxCount) * 100) : 0,
      colorClass: this.toColorClass(item.evidenceLevel),
    }));
  });

  private readonly dashboardService = inject(DashboardService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.loadEvidenceLevels();
  }

  private loadEvidenceLevels(): void {
    this.loading.set(true);
    this.error.set(null);

    this.dashboardService.getEvidenceLevels()
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
}

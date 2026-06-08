import {computed, DestroyRef, Directive, effect, inject, input, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {HttpErrorResponse} from '@angular/common/http';
import {Router} from '@angular/router';
import {Subscription} from 'rxjs';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {EvidenceLevelItem} from '@core/models/analytics.model';
import {AnalyticsProvider} from '@shared/components/analytics/analytics-provider';
import {GenesStore} from '@features/genes/state/filters.store';
import {EvidenceLevel} from '@core/models/protein.model';

@Directive()
export abstract class AbstractDashboardEvidenceLevelsDirective {
  public readonly filter = input<GeneFilterSnapshot | undefined>(undefined);
  protected readonly loading = signal<boolean>(true);
  protected readonly error = signal<string | null>(null);
  protected readonly evidenceItems = signal<ReadonlyArray<EvidenceLevelItem>>([]);
  protected readonly hasData = computed<boolean>(() => this.evidenceItems().length > 0);
  protected readonly sortedItems = computed<ReadonlyArray<EvidenceLevelItem>>(() => {
    return [...this.evidenceItems()].sort((left, right) => left.evidenceLevel - right.evidenceLevel);
  });
  protected readonly analyticProvider = inject(AnalyticsProvider);
  protected readonly genesStore = inject(GenesStore);
  protected readonly router = inject(Router);
  protected readonly destroyRef = inject(DestroyRef);
  private evidenceSub?: Subscription;

  constructor() {
    effect(() => {
      this.loadEvidenceLevels();
    });
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

  protected toColorClass(evidenceLevel: number): string {
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

  private loadEvidenceLevels(): void {
    this.evidenceSub?.unsubscribe();
    this.loading.set(true);
    this.error.set(null);

    this.evidenceSub = this.analyticProvider.getEvidenceLevels(this.filter())
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

  private isEvidenceLevel(level: number): level is EvidenceLevel {
    return level >= 1 && level <= 5;
  }
}

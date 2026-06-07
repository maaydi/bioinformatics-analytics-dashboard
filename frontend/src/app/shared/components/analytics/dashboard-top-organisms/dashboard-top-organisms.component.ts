import {DecimalPipe} from '@angular/common';
import {HttpErrorResponse} from '@angular/common/http';
import {ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {OrganismCount} from '@core/models/analytics.model';
import {LoadingSpinnerComponent} from '@shared/components/loading-spinner/loading-spinner.component';
import {Router} from '@angular/router';
import {GenesStore} from '@features/genes/state/filters.store';
import {AnalyticsProvider} from '@shared/components/analytics/analytics-provider';
import {LimitSelectorComponent} from '@shared/components/limit-selector/limit-selector.component';

interface OrganismView {
  readonly name: string;
  readonly count: number;
  readonly ratioPercent: number;
}

@Component({
  selector: 'app-dashboard-top-organisms',
  imports: [MatCardModule, DecimalPipe, LoadingSpinnerComponent, MatButtonModule, LimitSelectorComponent],
  templateUrl: './dashboard-top-organisms.component.html',
  styleUrl: './dashboard-top-organisms.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardTopOrganismsComponent {
  protected readonly loading = signal<boolean>(true);
  protected readonly error = signal<string | null>(null);
  private readonly organismsResponse = signal<ReadonlyArray<OrganismCount>>([]);

  protected readonly hasData = computed<boolean>(() => this.organismsResponse().length > 0);
  protected readonly organisms = computed<ReadonlyArray<OrganismView>>(() => {
    const maxCount = Math.max(...this.organismsResponse().map((item) => item.total), 0);
    return this.organismsResponse().map((item) => ({
      name: item.organismName,
      count: item.total,
      ratioPercent: maxCount > 0 ? Math.round((item.total / maxCount) * 100) : 0,
    }));
  });

  private readonly _limit = signal<number>(50);
  public readonly limit = this._limit.asReadonly();

  private readonly analyticProvider = inject(AnalyticsProvider);
  private readonly destroyRef = inject(DestroyRef);
  private readonly genesStore = inject(GenesStore);
  private readonly router = inject(Router);

  constructor() {
    this.loadTopOrganisms();
  }

  protected retry(): void {
    this.loadTopOrganisms();
  }

  protected selectOrganism(organismName: string): void {
    const snapshot = {organism: organismName};
    this.genesStore.setActiveFilters(snapshot);
    void this.router.navigate(['/genes']);
  }

  protected onLimitChanged(newLimit: number): void {
    if (this._limit() === newLimit) return;

    this._limit.set(newLimit);
    this.loadTopOrganisms();
  }

  private loadTopOrganisms(): void {
    this.loading.set(true);
    this.error.set(null);

    this.analyticProvider.getByOrganism(this._limit())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.organismsResponse.set(response);
          this.loading.set(false);
        },
        error: (_error: HttpErrorResponse) => {
          this.organismsResponse.set([]);
          this.error.set('Unable to load top organisms.');
          this.loading.set(false);
        }
      });
  }
}

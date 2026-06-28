import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  signal,
  viewChild
} from '@angular/core';
import {
  DashboardKpiCardListComponent
} from '@shared/components/analytics/dashboard-kpi-card-list/dashboard-kpi-card-list.component';
import {
  DashboardLengthHistogramComponent
} from '@shared/components/analytics/dashboard-length-histogram/dashboard-length-histogram.component';
import {
  DashboardReviewedRatioComponent
} from '@shared/components/analytics/dashboard-reviewed-ratio/dashboard-reviewed-ratio.component';
import {
  DashboardTopOrganismsComponent
} from '@shared/components/analytics/dashboard-top-organisms/dashboard-top-organisms.component';
import {AnalyticsProvider} from '@shared/components/analytics/analytics-provider';
import {AnalyticsService} from '@features/analytics/analytics.service';
import {
  DashboardEvidenceLevelsPieChartComponent
} from '@shared/components/analytics/dashboard-evidence-levels-pie-chart/dashboard-evidence-levels-pie-chart.component';
import {
  DashboardKeywordFrequencyHistogramComponent
} from '@shared/components/analytics/dashboard-keyword-frequency-histogram/dashboard-keyword-frequency-histogram.component';
import {SavedFilter} from '@core/models/saved-filter.model';
import {MatIcon} from '@angular/material/icon';
import {SavedFiltersService} from '@features/saved-filters/saved-filters.service';
import {MatCard} from '@angular/material/card';
import {LoadingSpinnerComponent} from '@shared/components/loading-spinner/loading-spinner.component';
import {
  DashboardScatterLengthWeightComponent
} from '@features/analytics/dashboard/dashboard-scatter-length-weight/dashboard-scatter-length-weight.component';
import {MatButton} from '@angular/material/button';
import html2canvas from 'html2canvas';
import {CompareComponent} from '@features/analytics/compare/compare.component';

@Component({
  selector: 'app-analytics',
  imports: [
    DashboardKpiCardListComponent,
    DashboardLengthHistogramComponent,
    DashboardReviewedRatioComponent,
    DashboardTopOrganismsComponent,
    DashboardEvidenceLevelsPieChartComponent,
    DashboardKeywordFrequencyHistogramComponent,
    MatIcon,
    MatCard,
    LoadingSpinnerComponent,
    DashboardScatterLengthWeightComponent,
    MatButton,
    CompareComponent,
  ],
  providers: [{provide: AnalyticsProvider, useExisting: AnalyticsService}],
  templateUrl: './analytics.component.html',
  styleUrls: ['./analytics.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AnalyticsComponent {

  readonly captureArea = viewChild.required(ElementRef);

  readonly compareComp = viewChild.required(CompareComponent);

  protected dashboardKpiCardLoading = signal<boolean>(true);

  /** True while loading saved filters from API. */
  protected loading = signal<boolean>(true);

  /** Array of user's saved filter sets. */
  protected filters = signal<SavedFilter[]>([]);

  protected selectedFilter = signal<SavedFilter | null>(null);

  /** User-facing error message, or null if no error. */
  protected errors = signal<string | null>(null);

  private readonly service = inject(SavedFiltersService);

  /** Controls view layout switcher state */
  protected isCompareMode = signal<boolean>(false);

  /** Automatically derives page title dynamically based on layout state */
  protected title = computed(() => this.isCompareMode() ? 'Compare Filter Sets' : 'Analytics');

  /**
   * Load saved filters on component init.
   */
  constructor() {
    afterNextRender(() => {
      this.loadAllFilters();
    });
  }

  protected onFilterSelect(event: Event): void {
    const selectElement = event.target as HTMLSelectElement;
    const filterId = Number(selectElement.value);

    if (!filterId) {
      this.selectedFilter.set(null);
      return;
    }

    const matchedFilter = this.filters().find((f) => f.id === filterId);
    this.selectedFilter.set(matchedFilter ?? null);
  }

  protected async exportDashboardAsImage() {
    if (!this.captureArea) return;

    const canvas = await html2canvas(this.captureArea().nativeElement, {
      scale: 2,
      backgroundColor: '#ffffff',
    });

    const image = canvas.toDataURL('image/png');

    const link = document.createElement('a');
    link.href = image;
    link.download = `analytics-dashboard-${this.selectedFilter()?.name.replaceAll(' ', '_')}-${new Date().toISOString()}.png`;
    link.click();
  }

  /**
   * Toggles the interactive layout view between Analytics and Comparison mode.
   */
  protected switchMode(): void {
    this.isCompareMode.update(mode => !mode);
  }

  private loadAllFilters(page: number = 0, accumulatedFilters: SavedFilter[] = []) {
    // Max page size supported by API
    const size = 200;
    this.service.listSavedFilters(page, size).subscribe({
      next: (sf) => {
        const allFilters = [...accumulatedFilters, ...(sf.content || [])];
        const isLastPage = sf.totalPages === page + 1;
        if (isLastPage) {
          this.filters.set(allFilters);
          this.errors.set(null);
          this.loading.set(false);
        } else {
          this.loadAllFilters(page + 1, allFilters);
        }
      },
      error: (_) => {
        this.errors.set('Failed to load saved filters');
        this.loading.set(false);
      }
    });
  }

}

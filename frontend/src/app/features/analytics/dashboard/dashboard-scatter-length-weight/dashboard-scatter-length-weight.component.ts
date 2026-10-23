import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  input,
  model,
  PLATFORM_ID,
  signal,
  ViewChild,
} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {HttpErrorResponse} from '@angular/common/http';
import {Subscription} from 'rxjs';
import {EChartsOption} from 'echarts';
import {ECElementEvent, ECharts} from 'echarts/core';
import {GeneSearchRequest} from '@core/models/saved-filter.model';
import {AnalyticsService} from '@features/analytics/analytics.service';
import {NGX_ECHARTS_CONFIG, NgxEchartsDirective} from 'ngx-echarts';
import {GenesStore} from '@features/genes/state/filters.store';
import {Router} from '@angular/router';
import {MatIcon} from '@angular/material/icon';
import {MatCard, MatCardContent} from '@angular/material/card';
import {MatButton, MatIconButton} from '@angular/material/button';
import {ImageExportService} from '@shared/directive/image-export-service';
import {isPlatformBrowser} from '@angular/common';
import {DASHBOARD_SCATTER_LENGTH_WEIGHT_MESSAGES} from './dashboard-scatter-length-weight.messages';

function isScatterPoint(value: unknown): value is readonly [number, number, number] {
  return Array.isArray(value) && value.length === 3 &&
    value.every((coordinate: unknown) => typeof coordinate === 'number' && Number.isFinite(coordinate));
}

@Component({
  selector: 'app-dashboard-scatter-length-weight',
  templateUrl: './dashboard-scatter-length-weight.component.html',
  imports: [NgxEchartsDirective, MatIcon, MatCard, MatIconButton, MatCardContent, MatButton],
  providers: [
    {
      provide: NGX_ECHARTS_CONFIG,
      useFactory: () => ({echarts: () => import('echarts')}), // Lazy load echarts
    },
  ],
  styleUrls: ['./dashboard-scatter-length-weight.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardScatterLengthWeightComponent {
  protected readonly messages = DASHBOARD_SCATTER_LENGTH_WEIGHT_MESSAGES;
  @ViewChild('chartCard', {read: ElementRef})
  chartCard!: ElementRef<HTMLElement>;

  public readonly filter = input<GeneSearchRequest | undefined>(undefined);
  public readonly rawLoading = model<boolean>(true);

  protected readonly scatterData = signal<ReadonlyArray<[number, number, number]>>([]);
  protected readonly chartError = signal<string | null>(null);
  protected readonly chartOptions = computed<EChartsOption>(() => {
    const data = this.scatterData();
    const counts = data.map((item) => item[2]);
    const maxCount =
      counts.length > 0 ? counts.reduce((max, current) => Math.max(max, current), -Infinity) : 10;
    return {
      grid: {
        top: 20,
        bottom: 60,
        left: 60,
        right: 80,
      },
      visualMap: {
        show: true,
        dimension: 2,
        min: 1,
        max: maxCount,
        itemHeight: 120,
        calculable: true,
        orient: 'vertical',
        right: 10,
        top: 'center',
        text: [this.messages.high, this.messages.low],
        inRange: {
          color: ['#a5b4fc', '#080117'],
        },
        outOfRange: {
          color: '#d1d5db',
          opacity: 0.1,
        },
      },
      tooltip: {
        trigger: 'item',
        axisPointer: {type: 'cross'},
        formatter: (params) => {
          const point = Array.isArray(params) ? params[0] : params;
          const value: unknown = point?.value;
          if (!isScatterPoint(value)) return '';

          const [length, weight, count] = value;
          return `
            <div style="font-weight: bold; margin-bottom: 4px;">${this.messages.tooltipTitle}</div>
            ${this.messages.lengthLabel} <b>${length}</b><br/>
            ${this.messages.weightLabel} <b>${this.messages.weight(weight)}</b><br/>
            ${this.messages.countLabel} <b>${this.messages.proteins(count)}</b>
          `;
        },
      },
      xAxis: {
        type: 'value',
        name: this.messages.lengthAxis,
        nameLocation: 'middle',
        nameGap: 30,
        splitLine: {show: true, lineStyle: {type: 'dashed'}},
      },
      yAxis: {
        type: 'value',
        name: this.messages.weightAxis,
        nameLocation: 'middle',
        nameGap: 45,
        splitLine: {show: true, lineStyle: {type: 'dashed'}},
      },
      series: [
        {
          type: 'scatter',
          data: data as Array<[number, number, number]>,
          symbolSize: 10,
          itemStyle: {
            opacity: 0.85,
          },
          emphasis: {
            itemStyle: {
              color: '#ef4444',
              symbolSize: 14,
            },
          },
        },
      ],
    };
  });
  private echartsInstance!: ECharts;
  private readonly imageExportService = inject(ImageExportService);

  private readonly analyticsService = inject(AnalyticsService);
  private readonly destroyRef = inject(DestroyRef);
  private chartSub?: Subscription;
  private readonly genesStore = inject(GenesStore);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);

  constructor() {
    effect(() => {
      if (isPlatformBrowser(this.platformId)) {
        this.loadChartData();
      }
    });
  }

  onChartInit(ec: ECharts): void {
    this.echartsInstance = ec;
  }

  protected retryChart(): void {
    this.loadChartData();
  }

  protected onChartClick(event: ECElementEvent): void {
    const data: unknown = event.data;
    if (!isScatterPoint(data)) return;

    const snapshot: GeneSearchRequest = {
      lengthMin: data[0],
      lengthMax: data[0],
      molecularWeightMin: data[1],
      molecularWeightMax: data[1],
    };
    this.genesStore.setActiveFilters(snapshot);
    void this.router.navigate(['/genes']);
  }

  private loadChartData(): void {
    this.chartSub?.unsubscribe();
    this.rawLoading.set(true);
    this.chartError.set(null);

    this.chartSub = this.analyticsService
      .getProteinLengthWeightCount(this.filter())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (molecules) => {
          this.scatterData.set(molecules.map((m) => [m.length, m.moleculeWeight, m.count]));
          this.rawLoading.set(false);
        },
        error: (_error: HttpErrorResponse) => {
          this.scatterData.set([]);
          this.chartError.set(this.messages.loadFailed);
          this.rawLoading.set(false);
        },
      });
  }


  protected async exportAsImage(): Promise<void> {
    if (!this.echartsInstance) return;
    await this.imageExportService.exportEChart(
      this.chartCard.nativeElement,
      this.echartsInstance,
      'protein-length-weight',
      '.no-export'
    );
  }
}

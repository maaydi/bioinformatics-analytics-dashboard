import {Component, computed, DestroyRef, effect, inject, input, model, signal,} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {HttpErrorResponse} from '@angular/common/http';
import {Subscription} from 'rxjs';
import {ECElementEvent, EChartsOption} from 'echarts';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {AnalyticsService} from '@features/analytics/analytics.service';
import {NGX_ECHARTS_CONFIG, NgxEchartsDirective} from 'ngx-echarts';
import {GenesStore} from '@features/genes/state/filters.store';
import {Router} from '@angular/router';

@Component({
  selector: 'app-dashboard-scatter-length-weight',
  templateUrl: './dashboard-scatter-length-weight.component.html',
  imports: [NgxEchartsDirective],
  providers: [
    {
      provide: NGX_ECHARTS_CONFIG,
      useFactory: () => ({echarts: () => import('echarts')}), // Lazy load echarts
    },
  ],
  styleUrls: ['./dashboard-scatter-length-weight.component.scss'],
})
export class DashboardScatterLengthWeightComponent {
  public readonly filter = input<GeneFilterSnapshot | undefined>(undefined);
  public readonly rawLoading = model<boolean>(true);

  protected readonly scatterData = signal<ReadonlyArray<[number, number, number]>>([]);
  protected readonly chartError = signal<string | null>(null);
  protected readonly chartOptions = computed<EChartsOption>(() => {
    return {
      title: {
        text: 'Molecular Weight vs. Sequence Length',
        subtext: 'Click a point for detailed analysis in Gene Explorer',
        left: 'center',
        textStyle: {fontSize: 16, color: '#333'},
      },
      grid: {
        top: 60,
        bottom: 60,
        left: 60,
        right: 40,
      },
      tooltip: {
        trigger: 'item',
        axisPointer: {type: 'cross'},
        formatter: (params: any) => {
          return `
            <div style="font-weight: bold; margin-bottom: 4px;">Molecule Stat</div>
            Length: <b>${params.value[0]}</b><br/>
            Weight: <b>${params.value[1]} Da</b><br/>
            Count: <b>${params.value[2]} protein${params.value[2] > 1 ? 's' : ''}</b>
          `;
        },
      },
      xAxis: {
        type: 'value',
        name: 'Length',
        nameLocation: 'middle',
        nameGap: 30,
        splitLine: {show: true, lineStyle: {type: 'dashed'}},
      },
      yAxis: {
        type: 'value',
        name: 'Molecular Weight (Da)',
        nameLocation: 'middle',
        nameGap: 45,
        splitLine: {show: true, lineStyle: {type: 'dashed'}},
      },
      series: [
        {
          type: 'scatter',
          data: this.scatterData() as Array<[number, number, number]>,
          symbolSize: 10,
          itemStyle: {
            color: '#4f46e5',
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
  private readonly analyticsService = inject(AnalyticsService);
  private readonly destroyRef = inject(DestroyRef);
  private chartSub?: Subscription;
  private readonly genesStore = inject(GenesStore);
  private readonly router = inject(Router);

  constructor() {
    effect(() => {
      this.loadChartData();
    });
  }

  protected retryChart(): void {
    this.loadChartData();
  }

  protected onChartClick(event: any) {
    const echartsEvent = event as ECElementEvent;
    const data: number[] = echartsEvent.data as number[];
    const snapshot: GeneFilterSnapshot = {
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
          this.chartError.set('Unable to load chart data.');
          this.rawLoading.set(false);
        },
      });
  }
}

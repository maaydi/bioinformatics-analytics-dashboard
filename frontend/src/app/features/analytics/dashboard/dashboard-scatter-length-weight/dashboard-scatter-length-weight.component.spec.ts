import {Directive, input, output} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {Subject} from 'rxjs';
import {EChartsOption, TooltipComponentFormatterCallbackParams} from 'echarts';
import {ECElementEvent, ECharts} from 'echarts/core';
import {NgxEchartsDirective} from 'ngx-echarts';
import {AnalyticsService} from '@features/analytics/analytics.service';
import {ProteinLengthWeightCount} from '@core/models/analytics.model';
import {DashboardScatterLengthWeightComponent} from './dashboard-scatter-length-weight.component';

@Directive({selector: '[echarts]'})
class ChartStubDirective {
  readonly options = input.required<EChartsOption>();
  readonly chartClick = output<ECElementEvent>();
  readonly chartInit = output<ECharts>();
}

describe('DashboardScatterLengthWeightComponent', () => {
  let component: DashboardScatterLengthWeightComponent;
  let fixture: ComponentFixture<DashboardScatterLengthWeightComponent>;
  let response: Subject<ProteinLengthWeightCount[]>;

  beforeEach(async () => {
    response = new Subject<ProteinLengthWeightCount[]>();
    await TestBed.configureTestingModule({
      imports: [DashboardScatterLengthWeightComponent],
      providers: [{
        provide: AnalyticsService,
        useValue: {getProteinLengthWeightCount: vi.fn(() => response.asObservable())},
      }],
    }).overrideComponent(DashboardScatterLengthWeightComponent, {
      remove: {imports: [NgxEchartsDirective]},
      add: {imports: [ChartStubDirective]},
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardScatterLengthWeightComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('renders its heading, hint, loading message, and accessible export action', () => {
    const element: HTMLElement = fixture.nativeElement;

    expect(element.querySelector('h2')?.textContent).toBe('Molecular Weight vs. Sequence Length');
    expect(element.textContent).toContain('Click a point for detailed analysis in Gene Explorer');
    expect(element.querySelector('.state-message')?.textContent).toBe('Loading scatter plot...');
    expect(element.querySelector('button')?.getAttribute('aria-label')).toBe('Export chart as image');
    expect(element.querySelector('button')?.getAttribute('title')).toBe('Export as Image');
  });

  it('renders the empty message after a successful response without points', async () => {
    response.next([]);
    await fixture.whenStable();
    const element: HTMLElement = fixture.nativeElement;

    expect(element.querySelector('.state-message')?.textContent).toBe('No molecular data available for plotting.');
  });

  it('renders the failure message and retry action', async () => {
    response.error(new Error('Unavailable'));
    await fixture.whenStable();
    const element: HTMLElement = fixture.nativeElement;

    expect(element.querySelector('[role="alert"] p')?.textContent).toBe('Unable to load chart data.');
    expect(element.querySelector('[role="alert"] button')?.textContent).toBe('Retry');
  });

  it('supplies externalized axis, visual-map, and tooltip messages to the chart', async () => {
    response.next([{length: 100, moleculeWeight: 12000, count: 2}]);
    await fixture.whenStable();
    const chart = fixture.debugElement.query(By.directive(ChartStubDirective)).injector.get(ChartStubDirective);
    const options = chart.options();

    expect(options.xAxis).toMatchObject({name: 'Length'});
    expect(options.yAxis).toMatchObject({name: 'Molecular Weight (Da)'});
    expect(options.visualMap).toMatchObject({text: ['High', 'Low']});

    const tooltip = Array.isArray(options.tooltip) ? options.tooltip[0] : options.tooltip;
    const formatter = tooltip?.formatter;
    if (typeof formatter !== 'function') throw new Error('Expected a tooltip formatter');

    const params: TooltipComponentFormatterCallbackParams = {
      componentType: 'series', componentSubType: 'scatter', componentIndex: 0,
      name: '', dataIndex: 0, data: [100, 12000, 2], value: [100, 12000, 2], $vars: [],
    };
    const plural = formatter(params, '', () => undefined);
    expect(plural).toContain('Molecule Stat');
    expect(plural).toContain('Length: <b>100</b>');
    expect(plural).toContain('Weight: <b>12000 Da</b>');
    expect(plural).toContain('Count: <b>2 proteins</b>');
    expect(formatter({...params, value: [100, 12000, 1]}, '', () => undefined)).toContain('Count: <b>1 protein</b>');
    expect(formatter({...params, value: ['invalid', 12000, 1]}, '', () => undefined)).toBe('');
  });
});

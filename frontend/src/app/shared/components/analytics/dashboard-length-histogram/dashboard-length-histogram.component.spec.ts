import {ComponentFixture, TestBed} from '@angular/core/testing';
import {DashboardLengthHistogramComponent} from './dashboard-length-histogram.component';
import {LengthHistogramBucket} from '@core/models/analytics.model';
import {Observable, of, Subject, throwError} from 'rxjs';
import {HttpErrorResponse} from '@angular/common/http';
import {vi} from 'vitest';
import {Router} from '@angular/router';
import {GenesStore} from '@features/genes/state/filters.store';
import {AnalyticsProvider} from '@shared/components/analytics/analytics-provider';

describe('DashboardLengthHistogramComponent', () => {
  let fixture: ComponentFixture<DashboardLengthHistogramComponent>;
  let analyticsProviderMock: Pick<AnalyticsProvider, 'getLengthHistogram'>;
  let genesStoreMock: { setActiveFilters: ReturnType<typeof vi.fn> };
  let routerMock: { navigate: ReturnType<typeof vi.fn> };

  const mockHistogram: LengthHistogramBucket[] = [
    {bucket: 1, rangeMin: 0, rangeMax: 99, count: 12000},
    {bucket: 2, rangeMin: 100, rangeMax: 199, count: 45000},
  ];

  const setup = (response$: Observable<LengthHistogramBucket[]>) => {
    vi.mocked(analyticsProviderMock.getLengthHistogram).mockReturnValue(response$);
    fixture = TestBed.createComponent(DashboardLengthHistogramComponent);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    analyticsProviderMock = {
      getLengthHistogram: vi.fn(),
    };

    genesStoreMock = {
      setActiveFilters: vi.fn(),
    };

    routerMock = {
      navigate: vi.fn(() => Promise.resolve(true)),
    };

    await TestBed.configureTestingModule({
      imports: [DashboardLengthHistogramComponent],
      providers: [
        {provide: AnalyticsProvider, useValue: analyticsProviderMock},
        {provide: GenesStore, useValue: genesStoreMock},
        {provide: Router, useValue: routerMock},
      ],
    }).compileComponents();
  });

  it('should show loading state while request is pending', () => {
    setup(new Subject<LengthHistogramBucket[]>().asObservable());
    expect(fixture.nativeElement.querySelector('app-loading-spinner')).toBeTruthy();
  });

  it('should render histogram bars from API data', () => {
    setup(of(mockHistogram));

    const text = fixture.nativeElement.textContent as string;
    const bars = fixture.nativeElement.querySelectorAll('.bar-col');
    const xAxisLabels = fixture.nativeElement.querySelectorAll('.x-label-sparse');

    expect(text).toContain('Protein Length Distribution');
    expect(text).toContain('Bar limit: 0 - 99 AA');
    expect(text).toContain('Bar limit: 100 - 199 AA');
    expect(text).toContain('Count: 12,000');
    expect(text).toContain('Count: 45,000');
    expect(text).toContain('Percentage: 21.05%');
    expect(text).toContain('Percentage: 78.95%');
    expect(xAxisLabels.length).toBe(3);
    expect(xAxisLabels[0].textContent?.trim()).toBe('0');
    expect(xAxisLabels[1].textContent?.trim()).toBe('100');
    expect(xAxisLabels[2].textContent?.trim()).toBe('199');
    expect(bars.length).toBe(2);
  });

  it('should render empty state when API returns no buckets', () => {
    setup(of([]));
    expect(fixture.nativeElement.textContent as string).toContain('No histogram data available.');
  });

  it('should keep bars keyboard-focusable for tooltip access', () => {
    setup(of(mockHistogram));

    const bars = fixture.nativeElement.querySelectorAll('.bar-col') as NodeListOf<HTMLElement>;
    expect(bars.length).toBe(2);
    for (const bar of Array.from(bars)) {
      expect(bar.tagName.toLowerCase()).toBe('button');
    }
  });

  it('should render error state when API fails', () => {
    setup(throwError(() => new HttpErrorResponse({status: 500, statusText: 'Server Error'})));
    expect(fixture.nativeElement.textContent as string).toContain('Unable to load protein length distribution.');
  });

  it('should retry loading histogram when retry is clicked', () => {
    vi.mocked(analyticsProviderMock.getLengthHistogram)
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({status: 500, statusText: 'Server Error'})))
      .mockReturnValueOnce(of(mockHistogram));

    fixture = TestBed.createComponent(DashboardLengthHistogramComponent);
    fixture.detectChanges();

    const retryButton = fixture.nativeElement.querySelector('button[mat-stroked-button]') as HTMLButtonElement;
    retryButton.click();
    fixture.detectChanges();

    expect(analyticsProviderMock.getLengthHistogram).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent as string).toContain('Count: 12,000');
  });

  it('should navigate to genes with length range filter when a bar is clicked', () => {
    setup(of(mockHistogram));

    const firstBar = fixture.nativeElement.querySelector('.bar-col') as HTMLButtonElement;
    firstBar.click();

    expect(genesStoreMock.setActiveFilters).toHaveBeenCalledWith({
      lengthMin: 0,
      lengthMax: 99,
    });
    expect(routerMock.navigate).toHaveBeenCalledWith(['/genes']);
  });

  it('should navigate to genes with correct range when second bar is clicked', () => {
    setup(of(mockHistogram));

    const secondBar = fixture.nativeElement.querySelectorAll('.bar-col')[1] as HTMLButtonElement;
    secondBar.click();

    expect(genesStoreMock.setActiveFilters).toHaveBeenCalledWith({
      lengthMin: 100,
      lengthMax: 199,
    });
    expect(routerMock.navigate).toHaveBeenCalledWith(['/genes']);
  });
});


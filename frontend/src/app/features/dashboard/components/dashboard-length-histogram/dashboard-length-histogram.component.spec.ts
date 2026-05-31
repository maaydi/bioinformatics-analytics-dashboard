import {ComponentFixture, TestBed} from '@angular/core/testing';
import {DashboardLengthHistogramComponent} from './dashboard-length-histogram.component';
import {DashboardService} from '@features/dashboard/dashboard.service';
import {LengthHistogramBucket} from '@core/models/analytics.model';
import {Observable, of, Subject, throwError} from 'rxjs';
import {HttpErrorResponse} from '@angular/common/http';
import {vi} from 'vitest';

describe('DashboardLengthHistogramComponent', () => {
  let fixture: ComponentFixture<DashboardLengthHistogramComponent>;
  let dashboardServiceMock: Pick<DashboardService, 'getLengthHistogram'>;

  const mockHistogram: LengthHistogramBucket[] = [
    {bucket: 1, rangeMin: 0, rangeMax: 99, count: 12000},
    {bucket: 2, rangeMin: 100, rangeMax: 199, count: 45000},
  ];

  const setup = (response$: Observable<LengthHistogramBucket[]>) => {
    vi.mocked(dashboardServiceMock.getLengthHistogram).mockReturnValue(response$);
    fixture = TestBed.createComponent(DashboardLengthHistogramComponent);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    dashboardServiceMock = {
      getLengthHistogram: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [DashboardLengthHistogramComponent],
      providers: [{provide: DashboardService, useValue: dashboardServiceMock}],
    }).compileComponents();
  });

  it('should show loading state while request is pending', () => {
    setup(new Subject<LengthHistogramBucket[]>().asObservable());
    expect(fixture.nativeElement.textContent as string).toContain('Loading chart data...');
  });

  it('should render histogram bars from API data', () => {
    setup(of(mockHistogram));

    const text = fixture.nativeElement.textContent as string;
    const bars = fixture.nativeElement.querySelectorAll('.bar-col');

    expect(text).toContain('Protein Length Distribution');
    expect(text).toContain('0-99');
    expect(text).toContain('100-199');
    expect(bars.length).toBe(2);
  });

  it('should render empty state when API returns no buckets', () => {
    setup(of([]));
    expect(fixture.nativeElement.textContent as string).toContain('No histogram data available.');
  });

  it('should render error state when API fails', () => {
    setup(throwError(() => new HttpErrorResponse({status: 500, statusText: 'Server Error'})));
    expect(fixture.nativeElement.textContent as string).toContain('Unable to load protein length distribution.');
  });
});


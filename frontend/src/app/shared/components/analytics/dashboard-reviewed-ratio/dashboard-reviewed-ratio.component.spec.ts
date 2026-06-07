import {ComponentFixture, TestBed} from '@angular/core/testing';
import {DashboardReviewedRatioComponent} from './dashboard-reviewed-ratio.component';
import {Observable, of, Subject, throwError} from 'rxjs';
import {ReviewedRatioItem} from '@core/models/analytics.model';
import {HttpErrorResponse} from '@angular/common/http';
import {vi} from 'vitest';
import {Router} from '@angular/router';
import {GenesStore} from '@features/genes/state/filters.store';
import {AnalyticsProvider} from '@shared/components/analytics/analytics-provider';

describe('DashboardReviewedRatioComponent', () => {
  let fixture: ComponentFixture<DashboardReviewedRatioComponent>;
  let analyticProviderMock: Pick<AnalyticsProvider, 'getReviewedRatio'>;
  let genesStoreMock: { setActiveFilters: ReturnType<typeof vi.fn> };
  let routerMock: { navigate: ReturnType<typeof vi.fn> };

  const mockRatio: ReviewedRatioItem[] = [
    {reviewed: true, count: 80},
    {reviewed: false, count: 20},
  ];

  const setup = (response$: Observable<ReviewedRatioItem[]>) => {
    vi.mocked(analyticProviderMock.getReviewedRatio).mockReturnValue(response$);
    fixture = TestBed.createComponent(DashboardReviewedRatioComponent);
    fixture.detectChanges();
  };

  beforeEach(() => {
    analyticProviderMock = {
      getReviewedRatio: vi.fn(),
    };

    genesStoreMock = {
      setActiveFilters: vi.fn(),
    };

    routerMock = {
      navigate: vi.fn(() => Promise.resolve(true)),
    };
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardReviewedRatioComponent],
      providers: [
        {provide: AnalyticsProvider, useValue: analyticProviderMock},
        {provide: GenesStore, useValue: genesStoreMock},
        {provide: Router, useValue: routerMock},
      ],
    }).compileComponents();
  });


  it('should show loading state while request is pending', () => {
    setup(new Subject<ReviewedRatioItem[]>().asObservable());
    expect(fixture.nativeElement.querySelector('app-loading-spinner')).toBeTruthy();
  });

  it('should render reviewed and unreviewed values from API data', () => {
    setup(of(mockRatio));

    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Reviewed: 80 (80%)');
    expect(text).toContain('Unreviewed: 20 (20%)');
  });

  it('should render empty state when ratio is empty', () => {
    setup(of([]));
    expect(fixture.nativeElement.textContent as string).toContain('No reviewed ratio data available.');
  });

  it('should render error state when API fails', () => {
    setup(throwError(() => new HttpErrorResponse({status: 500, statusText: 'Server Error'})));
    expect(fixture.nativeElement.textContent as string).toContain('Unable to load reviewed ratio data.');
  });

  it('should retry loading ratio when retry is clicked', () => {
    vi.mocked(analyticProviderMock.getReviewedRatio)
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({status: 500, statusText: 'Server Error'})))
      .mockReturnValueOnce(of(mockRatio));

    fixture = TestBed.createComponent(DashboardReviewedRatioComponent);
    fixture.detectChanges();

    const retryButton = fixture.nativeElement.querySelector('button[mat-stroked-button]') as HTMLButtonElement;
    retryButton.click();
    fixture.detectChanges();

    expect(analyticProviderMock.getReviewedRatio).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent as string).toContain('Reviewed: 80 (80%)');
  });

  it('should navigate to genes with reviewed=true filter when Reviewed legend is clicked', () => {
    setup(of(mockRatio));

    const reviewedButton = Array.from(fixture.nativeElement.querySelectorAll('button.legend-button'))
      .find((btn: any) => btn.textContent.includes('Reviewed:')) as HTMLButtonElement;
    reviewedButton.click();

    expect(genesStoreMock.setActiveFilters).toHaveBeenCalledWith({reviewed: true});
    expect(routerMock.navigate).toHaveBeenCalledWith(['/genes']);
  });

  it('should navigate to genes with reviewed=false filter when Unreviewed legend is clicked', () => {
    setup(of(mockRatio));

    const unreviewedButton = Array.from(fixture.nativeElement.querySelectorAll('button.legend-button'))
      .find((btn: any) => btn.textContent.includes('Unreviewed:')) as HTMLButtonElement;
    unreviewedButton.click();

    expect(genesStoreMock.setActiveFilters).toHaveBeenCalledWith({reviewed: false});
    expect(routerMock.navigate).toHaveBeenCalledWith(['/genes']);
  });
});


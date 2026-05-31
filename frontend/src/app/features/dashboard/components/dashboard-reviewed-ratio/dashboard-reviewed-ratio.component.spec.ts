import {ComponentFixture, TestBed} from '@angular/core/testing';
import {DashboardReviewedRatioComponent} from './dashboard-reviewed-ratio.component';
import {DashboardService} from '@features/dashboard/dashboard.service';
import {Observable, of, Subject, throwError} from 'rxjs';
import {ReviewedRatioItem} from '@core/models/analytics.model';
import {HttpErrorResponse} from '@angular/common/http';
import {vi} from 'vitest';

describe('DashboardReviewedRatioComponent', () => {
  let fixture: ComponentFixture<DashboardReviewedRatioComponent>;
  let dashboardServiceMock: Pick<DashboardService, 'getReviewedRatio'>;

  const mockRatio: ReviewedRatioItem[] = [
    {reviewed: true, count: 80},
    {reviewed: false, count: 20},
  ];

  const setup = (response$: Observable<ReviewedRatioItem[]>) => {
    vi.mocked(dashboardServiceMock.getReviewedRatio).mockReturnValue(response$);
    fixture = TestBed.createComponent(DashboardReviewedRatioComponent);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    dashboardServiceMock = {
      getReviewedRatio: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [DashboardReviewedRatioComponent],
      providers: [{provide: DashboardService, useValue: dashboardServiceMock}],
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
    vi.mocked(dashboardServiceMock.getReviewedRatio)
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({status: 500, statusText: 'Server Error'})))
      .mockReturnValueOnce(of(mockRatio));

    fixture = TestBed.createComponent(DashboardReviewedRatioComponent);
    fixture.detectChanges();

    const retryButton = fixture.nativeElement.querySelector('button[mat-stroked-button]') as HTMLButtonElement;
    retryButton.click();
    fixture.detectChanges();

    expect(dashboardServiceMock.getReviewedRatio).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent as string).toContain('Reviewed: 80 (80%)');
  });
});


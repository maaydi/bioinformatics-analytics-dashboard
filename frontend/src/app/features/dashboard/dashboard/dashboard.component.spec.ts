import {ComponentFixture, TestBed} from '@angular/core/testing';
import {DashboardComponent} from './dashboard.component';
import {DashboardService} from '@features/dashboard/dashboard.service';
import {
  DashboardKpis,
  EvidenceLevelItem,
  LengthHistogramBucket,
  OrganismCount,
  ReviewedRatioItem,
} from '@core/models/analytics.model';
import {Observable, of, Subject, throwError} from 'rxjs';
import {HttpErrorResponse} from '@angular/common/http';
import {vi} from 'vitest';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let dashboardServiceMock: Pick<DashboardService,
    'getDashboardKpis' | 'getLengthHistogram' | 'getReviewedRatio' | 'getEvidenceLevels' | 'getByOrganism'>;

  const mockKpis: DashboardKpis = {
    totalProteins: 570000,
    reviewedCount: 300000,
    unreviewedCount: 270000,
    organismCount: 14822,
    taxonCount: 14822,
    avgLength: 360,
    avgMolecularWeight: 40643,
    minLength: 2,
    maxLength: 35213,
  };

  const childHistogram: LengthHistogramBucket[] = [
    {bucket: 1, rangeMin: 0, rangeMax: 99, count: 1000},
  ];

  const childRatio: ReviewedRatioItem[] = [
    {reviewed: true, count: 3},
    {reviewed: false, count: 2},
  ];

  const childEvidence: EvidenceLevelItem[] = [
    {evidenceLevel: 1, label: 'Protein level', count: 100},
  ];

  const childOrganisms: OrganismCount[] = [
    {
      organismName: 'Homo sapiens',
      taxid: 9606,
      total: 200,
      reviewedCount: 100,
      unreviewedCount: 100,
      avgLength: 400,
    }
  ];

  const setup = (response$: Observable<DashboardKpis>) => {
    vi.mocked(dashboardServiceMock.getDashboardKpis).mockReturnValue(response$);
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  beforeEach(async () => {
    dashboardServiceMock = {
      getDashboardKpis: vi.fn(),
      getLengthHistogram: vi.fn(() => of(childHistogram)),
      getReviewedRatio: vi.fn(() => of(childRatio)),
      getEvidenceLevels: vi.fn(() => of(childEvidence)),
      getByOrganism: vi.fn(() => of(childOrganisms)),
    };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [{provide: DashboardService, useValue: dashboardServiceMock}],
    }).compileComponents();
  });

  it('should create the component', () => {
    setup(of(mockKpis));
    expect(component).toBeTruthy();
  });

  it('should show loading state while KPI request is pending', () => {
    setup(new Subject<DashboardKpis>().asObservable());
    expect(fixture.nativeElement.textContent as string).toContain('Loading KPI cards...');
  });

  it('should render KPI cards from service response', () => {
    setup(of(mockKpis));
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Dashboard');
    expect(text).toContain('Total');
    expect(text).toContain('570,000');
    expect(text).toContain('Reviewed');
    expect(text).toContain('300,000');
    expect(text).toContain('Taxa');
    expect(text).toContain('Protein Length Distribution');
    expect(text).toContain('Evidence Levels');
    expect(text).toContain('Top 10 Organisms');
  });

  it('should render KPI error state when service fails', () => {
    setup(throwError(() => new HttpErrorResponse({status: 500, statusText: 'Server Error'})));
    expect(fixture.nativeElement.textContent as string).toContain('Unable to load dashboard KPIs.');
  });

  it('should render KPI empty state when service returns no values', () => {
    const emptyKpis: DashboardKpis = {
      ...mockKpis,
      totalProteins: 0,
      reviewedCount: 0,
      organismCount: 0,
      taxonCount: 0,
      avgLength: 0,
    };

    setup(of(emptyKpis));

    const kpiCards = fixture.nativeElement.querySelectorAll('app-dashboard-kpi-card');
    expect(kpiCards.length).toBe(5);
  });
});


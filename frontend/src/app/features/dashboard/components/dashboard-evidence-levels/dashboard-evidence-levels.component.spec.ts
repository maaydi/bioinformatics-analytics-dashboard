import {ComponentFixture, TestBed} from '@angular/core/testing';
import {DashboardEvidenceLevelsComponent} from './dashboard-evidence-levels.component';
import {DashboardService} from '@features/dashboard/dashboard.service';
import {EvidenceLevelItem} from '@core/models/analytics.model';
import {Observable, of, Subject, throwError} from 'rxjs';
import {HttpErrorResponse} from '@angular/common/http';
import {vi} from 'vitest';

describe('DashboardEvidenceLevelsComponent', () => {
  let fixture: ComponentFixture<DashboardEvidenceLevelsComponent>;
  let dashboardServiceMock: Pick<DashboardService, 'getEvidenceLevels'>;

  const mockEvidence: EvidenceLevelItem[] = [
    {evidenceLevel: 1, label: 'Protein level', count: 400000},
    {evidenceLevel: 2, label: 'Transcript level', count: 200000},
  ];

  const setup = (response$: Observable<EvidenceLevelItem[]>) => {
    vi.mocked(dashboardServiceMock.getEvidenceLevels).mockReturnValue(response$);
    fixture = TestBed.createComponent(DashboardEvidenceLevelsComponent);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    dashboardServiceMock = {
      getEvidenceLevels: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [DashboardEvidenceLevelsComponent],
      providers: [{provide: DashboardService, useValue: dashboardServiceMock}],
    }).compileComponents();
  });

  it('should show loading state while request is pending', () => {
    setup(new Subject<EvidenceLevelItem[]>().asObservable());
    expect(fixture.nativeElement.textContent as string).toContain('Loading chart data...');
  });

  it('should render evidence levels from API data', () => {
    setup(of(mockEvidence));

    const text = fixture.nativeElement.textContent as string;
    const rows = fixture.nativeElement.querySelectorAll('.row');

    expect(text).toContain('L1 - Protein level');
    expect(text).toContain('L2 - Transcript level');
    expect(rows.length).toBe(2);
  });

  it('should render empty state when API returns no levels', () => {
    setup(of([]));
    expect(fixture.nativeElement.textContent as string).toContain('No evidence level data available.');
  });

  it('should render error state when API fails', () => {
    setup(throwError(() => new HttpErrorResponse({status: 500, statusText: 'Server Error'})));
    expect(fixture.nativeElement.textContent as string).toContain('Unable to load evidence level distribution.');
  });
});


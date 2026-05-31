import {ComponentFixture, TestBed} from '@angular/core/testing';
import {DashboardEvidenceLevelsComponent} from './dashboard-evidence-levels.component';
import {DashboardService} from '@features/dashboard/dashboard.service';
import {EvidenceLevelItem} from '@core/models/analytics.model';
import {Observable, of, Subject, throwError} from 'rxjs';
import {HttpErrorResponse} from '@angular/common/http';
import {vi} from 'vitest';
import {GenesStore} from '@features/genes/state/filters.store';
import {Router} from '@angular/router';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';

interface GenesStoreMock {
  setActiveFilters(snapshot: GeneFilterSnapshot): void;
}

describe('DashboardEvidenceLevelsComponent', () => {
  let fixture: ComponentFixture<DashboardEvidenceLevelsComponent>;
  let dashboardServiceMock: Pick<DashboardService, 'getEvidenceLevels'>;
  let genesStoreMock: GenesStoreMock;
  let routerMock: Pick<Router, 'navigate'>;

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

    genesStoreMock = {
      setActiveFilters: vi.fn(),
    };

    routerMock = {
      navigate: vi.fn(() => Promise.resolve(true)),
    };

    await TestBed.configureTestingModule({
      imports: [DashboardEvidenceLevelsComponent],
      providers: [
        {provide: DashboardService, useValue: dashboardServiceMock},
        {provide: GenesStore, useValue: genesStoreMock},
        {provide: Router, useValue: routerMock},
      ],
    }).compileComponents();
  });

  it('should show loading state while request is pending', () => {
    setup(new Subject<EvidenceLevelItem[]>().asObservable());
    expect(fixture.nativeElement.querySelector('app-loading-spinner')).toBeTruthy();
  });

  it('should render evidence levels from API data', () => {
    setup(of(mockEvidence));

    const text = fixture.nativeElement.textContent as string;
    const rows = fixture.nativeElement.querySelectorAll('.row');

    expect(text).toContain('L1 - Protein level');
    expect(text).toContain('L2 - Transcript level');
    expect(rows.length).toBe(2);
  });

  it('should render levels from L1 at top to L5 at bottom', () => {
    setup(of([
      {evidenceLevel: 5, label: 'Uncertain', count: 50},
      {evidenceLevel: 1, label: 'Protein level', count: 400},
      {evidenceLevel: 3, label: 'Inferred from homology', count: 200},
    ]));

    const labels = Array.from(
      fixture.nativeElement.querySelectorAll('.row-label') as NodeListOf<HTMLElement>
    ).map((element) => element.textContent?.trim() ?? '');

    expect(labels).toEqual([
      'L1 - Protein level',
      'L3 - Inferred from homology',
      'L5 - Uncertain',
    ]);
  });

  it('should render empty state when API returns no levels', () => {
    setup(of([]));
    expect(fixture.nativeElement.textContent as string).toContain('No evidence level data available.');
  });

  it('should render error state when API fails', () => {
    setup(throwError(() => new HttpErrorResponse({status: 500, statusText: 'Server Error'})));
    expect(fixture.nativeElement.textContent as string).toContain('Unable to load evidence level distribution.');
  });

  it('should retry loading evidence levels when retry is clicked', () => {
    vi.mocked(dashboardServiceMock.getEvidenceLevels)
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({status: 500, statusText: 'Server Error'})))
      .mockReturnValueOnce(of(mockEvidence));

    fixture = TestBed.createComponent(DashboardEvidenceLevelsComponent);
    fixture.detectChanges();

    const retryButton = fixture.nativeElement.querySelector('button[mat-stroked-button]') as HTMLButtonElement;
    retryButton.click();
    fixture.detectChanges();

    expect(dashboardServiceMock.getEvidenceLevels).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent as string).toContain('L1 - Protein level');
  });

  it('should navigate to genes with the selected evidence filter when a row is clicked', () => {
    setup(of(mockEvidence));

    const firstRowButton = fixture.nativeElement.querySelector('.row-action') as HTMLButtonElement;
    firstRowButton.click();

    expect(genesStoreMock.setActiveFilters).toHaveBeenCalledWith({evidenceLevels: [1]});
    expect(routerMock.navigate).toHaveBeenCalledWith(['/genes']);
  });
});


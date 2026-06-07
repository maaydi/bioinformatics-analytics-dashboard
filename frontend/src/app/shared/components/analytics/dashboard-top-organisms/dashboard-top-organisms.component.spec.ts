import {ComponentFixture, TestBed} from '@angular/core/testing';
import {DashboardTopOrganismsComponent} from './dashboard-top-organisms.component';
import {OrganismCount} from '@core/models/analytics.model';
import {Observable, of, Subject, throwError} from 'rxjs';
import {HttpErrorResponse} from '@angular/common/http';
import {vi} from 'vitest';
import {Router} from '@angular/router';
import {GenesStore} from '@features/genes/state/filters.store';
import {AnalyticsProvider} from '@shared/components/analytics/analytics-provider';

describe('DashboardTopOrganismsComponent', () => {
  let fixture: ComponentFixture<DashboardTopOrganismsComponent>;
  let analyticProviderMock: Pick<AnalyticsProvider, 'getByOrganism'>;
  let genesStoreMock: { setActiveFilters: ReturnType<typeof vi.fn> };
  let routerMock: { navigate: ReturnType<typeof vi.fn> };

  const mockOrganisms: OrganismCount[] = [
    {
      organismName: 'Homo sapiens',
      taxid: 9606,
      total: 20581,
      reviewedCount: 20581,
      unreviewedCount: 0,
      avgLength: 480,
    },
    {
      organismName: 'Mus musculus',
      taxid: 10090,
      total: 18000,
      reviewedCount: 17000,
      unreviewedCount: 1000,
      avgLength: 420,
    },
  ];

  const setup = (response$: Observable<OrganismCount[]>) => {
    vi.mocked(analyticProviderMock.getByOrganism).mockReturnValue(response$);
    fixture = TestBed.createComponent(DashboardTopOrganismsComponent);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    analyticProviderMock = {
      getByOrganism: vi.fn(),
    };

    genesStoreMock = {
      setActiveFilters: vi.fn(),
    };

    routerMock = {
      navigate: vi.fn(() => Promise.resolve(true)),
    };

    await TestBed.configureTestingModule({
      imports: [DashboardTopOrganismsComponent],
      providers: [
        {provide: AnalyticsProvider, useValue: analyticProviderMock},
        {provide: GenesStore, useValue: genesStoreMock},
        {provide: Router, useValue: routerMock},
      ],
    }).compileComponents();
  });

  it('should show loading state while request is pending', () => {
    setup(new Subject<OrganismCount[]>().asObservable());
    expect(fixture.nativeElement.querySelector('app-loading-spinner')).toBeTruthy();
  });

  it('should render top organisms from API data', () => {
    setup(of(mockOrganisms));

    const text = fixture.nativeElement.textContent as string;
    const rows = fixture.nativeElement.querySelectorAll('.organism-row');

    expect(text).toContain('Homo sapiens');
    expect(text).toContain('20,581');
    expect(text).toContain('Mus musculus');
    expect(rows.length).toBe(2);
    expect(analyticProviderMock.getByOrganism).toHaveBeenCalledWith(50);
  });

  it('should render empty state when API returns no organisms', () => {
    setup(of([]));
    expect(fixture.nativeElement.textContent as string).toContain('No organism distribution data available.');
  });

  it('should render error state when API fails', () => {
    setup(throwError(() => new HttpErrorResponse({status: 500, statusText: 'Server Error'})));
    expect(fixture.nativeElement.textContent as string).toContain('Unable to load top organisms.');
  });

  it('should retry loading top organisms when retry is clicked', () => {
    vi.mocked(analyticProviderMock.getByOrganism)
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({status: 500, statusText: 'Server Error'})))
      .mockReturnValueOnce(of(mockOrganisms));

    fixture = TestBed.createComponent(DashboardTopOrganismsComponent);
    fixture.detectChanges();

    const retryButton = fixture.nativeElement.querySelector('button[mat-stroked-button]') as HTMLButtonElement;
    retryButton.click();
    fixture.detectChanges();

    expect(analyticProviderMock.getByOrganism).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent as string).toContain('Homo sapiens');
  });

  it('should navigate to genes with organism filter when Homo sapiens row is clicked', () => {
    setup(of(mockOrganisms));

    const firstRow = fixture.nativeElement.querySelector('.organism-row') as HTMLButtonElement;
    firstRow.click();

    expect(genesStoreMock.setActiveFilters).toHaveBeenCalledWith({organism: 'Homo sapiens'});
    expect(routerMock.navigate).toHaveBeenCalledWith(['/genes']);
  });

  it('should navigate to genes with correct organism when Mus musculus row is clicked', () => {
    setup(of(mockOrganisms));

    const secondRow = fixture.nativeElement.querySelectorAll('.organism-row')[1] as HTMLButtonElement;
    secondRow.click();

    expect(genesStoreMock.setActiveFilters).toHaveBeenCalledWith({organism: 'Mus musculus'});
    expect(routerMock.navigate).toHaveBeenCalledWith(['/genes']);
  });
});


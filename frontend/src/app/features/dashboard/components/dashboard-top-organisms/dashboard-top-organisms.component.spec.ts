import {ComponentFixture, TestBed} from '@angular/core/testing';
import {DashboardTopOrganismsComponent} from './dashboard-top-organisms.component';
import {DashboardService} from '@features/dashboard/dashboard.service';
import {OrganismCount} from '@core/models/analytics.model';
import {Observable, of, Subject, throwError} from 'rxjs';
import {HttpErrorResponse} from '@angular/common/http';
import {vi} from 'vitest';

describe('DashboardTopOrganismsComponent', () => {
  let fixture: ComponentFixture<DashboardTopOrganismsComponent>;
  let dashboardServiceMock: Pick<DashboardService, 'getByOrganism'>;

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
    vi.mocked(dashboardServiceMock.getByOrganism).mockReturnValue(response$);
    fixture = TestBed.createComponent(DashboardTopOrganismsComponent);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    dashboardServiceMock = {
      getByOrganism: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [DashboardTopOrganismsComponent],
      providers: [{provide: DashboardService, useValue: dashboardServiceMock}],
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
    expect(dashboardServiceMock.getByOrganism).toHaveBeenCalledWith(10);
  });

  it('should render empty state when API returns no organisms', () => {
    setup(of([]));
    expect(fixture.nativeElement.textContent as string).toContain('No organism distribution data available.');
  });

  it('should render error state when API fails', () => {
    setup(throwError(() => new HttpErrorResponse({status: 500, statusText: 'Server Error'})));
    expect(fixture.nativeElement.textContent as string).toContain('Unable to load top organisms.');
  });
});


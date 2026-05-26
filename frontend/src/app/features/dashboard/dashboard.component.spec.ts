import {ComponentFixture, TestBed} from '@angular/core/testing';
import {DashboardComponent} from './dashboard.component';
import {DashboardService} from './dashboard.service';
import {LoadingSpinnerComponent} from '@shared/components/loading-spinner/loading-spinner.component';
import {MatCardModule} from '@angular/material/card';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {vi} from 'vitest';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let dashboardService: DashboardService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent, LoadingSpinnerComponent, MatCardModule],
      providers: [DashboardService, provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    dashboardService = TestBed.inject(DashboardService);
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  // OnPush strategy is implicit in Angular 21+ standalone; skipping explicit test

  it('should be a standalone component', () => {
    const metadata = (DashboardComponent as any).ɵcmp;
    expect(metadata.standalone).toBe(true);
  });

  it('should call ngOnInit', () => {
    const spy = vi.spyOn(component, 'ngOnInit');
    component.ngOnInit();
    expect(spy).toHaveBeenCalled();
  });

  it('should render without errors', () => {
    expect(() => fixture.detectChanges()).not.toThrow();
  });

  it('should inject DashboardService', () => {
    expect(dashboardService).toBeTruthy();
  });
});


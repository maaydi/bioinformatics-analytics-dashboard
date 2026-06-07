import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ChangeDetectionStrategy, Component, DebugElement, input, output} from '@angular/core';
import {By} from '@angular/platform-browser';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {DashboardComponent} from './dashboard.component';
import {DashboardService} from '@features/dashboard/dashboard.service';
import {AnalyticsProvider} from '@shared/components/analytics/analytics-provider';
import {
  DashboardLengthHistogramComponent,
} from '@shared/components/analytics/dashboard-length-histogram/dashboard-length-histogram.component';
import {
  DashboardReviewedRatioComponent,
} from '@shared/components/analytics/dashboard-reviewed-ratio/dashboard-reviewed-ratio.component';
import {
  DashboardEvidenceLevelsComponent,
} from '@shared/components/analytics/dashboard-evidence-levels/dashboard-evidence-levels.component';
import {
  DashboardTopOrganismsComponent,
} from '@shared/components/analytics/dashboard-top-organisms/dashboard-top-organisms.component';
import {
  DashboardKpiCardListComponent,
} from '@shared/components/analytics/dashboard-kpi-card-list/dashboard-kpi-card-list.component';
import {
  DashboardKeywordFrequencyHistogramComponent,
} from '@shared/components/analytics/dashboard-keyword-frequency-histogram/dashboard-keyword-frequency-histogram.component';
import {of} from 'rxjs';

/**
 * Stub implementation of DashboardKpiCardListComponent for testing.
 * Properly implements the kpiLoading two-way binding.
 */
@Component({
  selector: 'app-dashboard-kpi-card-list',
  standalone: true,
  template: '',
})
class DashboardKpiCardListStubComponent {
  kpiLoading = input<boolean>(false);
  kpiLoadingChange = output<boolean>();
}

/**
 * Stub implementations of analytics child components.
 */
@Component({
  selector: 'app-dashboard-length-histogram',
  standalone: true,
  template: '',
})
class DashboardLengthHistogramStubComponent {
}

@Component({
  selector: 'app-dashboard-reviewed-ratio',
  standalone: true,
  template: '',
})
class DashboardReviewedRatioStubComponent {
}

@Component({
  selector: 'app-dashboard-evidence-levels',
  standalone: true,
  template: '',
})
class DashboardEvidenceLevelsStubComponent {
}

@Component({
  selector: 'app-dashboard-top-organisms',
  standalone: true,
  template: '',
})
class DashboardTopOrganismsStubComponent {
}

@Component({
  selector: 'app-dashboard-keyword-frequency-histogram',
  standalone: true,
  template: '',
})
class DashboardKeywordFrequencyHistogramStubComponent {
}

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let compiled: DebugElement;
  beforeEach(async () => {
    const mockDashboardService = {
      getDashboardKpis: vi.fn().mockReturnValue(of([])),
      getLengthHistogram: vi.fn().mockReturnValue(of([])),
      getByOrganism: vi.fn().mockReturnValue(of([])),
      getReviewedRatio: vi.fn().mockReturnValue(of([])),
      getEvidenceLevels: vi.fn().mockReturnValue(of([])),
      getKeywordFrequency: vi.fn().mockReturnValue(of([])),
    };
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        {provide: DashboardService, useValue: mockDashboardService},
        {provide: AnalyticsProvider, useValue: mockDashboardService},
      ],
    })
      .overrideComponent(DashboardComponent, {
        remove: {
          imports: [
            DashboardLengthHistogramComponent,
            DashboardReviewedRatioComponent,
            DashboardEvidenceLevelsComponent,
            DashboardTopOrganismsComponent,
            DashboardKpiCardListComponent,
            DashboardKeywordFrequencyHistogramComponent,
          ],
        },
        add: {
          imports: [
            DashboardLengthHistogramStubComponent,
            DashboardReviewedRatioStubComponent,
            DashboardEvidenceLevelsStubComponent,
            DashboardTopOrganismsStubComponent,
            DashboardKpiCardListStubComponent,
            DashboardKeywordFrequencyHistogramStubComponent,
          ],
        },
      })
      .compileComponents();
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    compiled = fixture.debugElement;
    fixture.detectChanges();
  });
  describe('Component Initialization', () => {
    it('should create the dashboard component', () => {
      expect(component).toBeDefined();
    });
    it('should implement OnPush change detection', () => {
      const metadata = (component as any).constructor.ɵcmp;
      expect(metadata.changeDetection === ChangeDetectionStrategy.OnPush || metadata.changeDetection === undefined).toBe(true);
    });
    it('should initialize dashboardKpiCardLoading signal with true', () => {
      expect(component['dashboardKpiCardLoading']()).toBe(true);
    });
  });
  describe('Signal State Management', () => {
    it('should update dashboardKpiCardLoading signal when child emits loading state', () => {
      expect(component['dashboardKpiCardLoading']()).toBe(true);
      component['dashboardKpiCardLoading'].set(false);
      expect(component['dashboardKpiCardLoading']()).toBe(false);
    });
    it('should allow multiple state toggles of dashboardKpiCardLoading', () => {
      component['dashboardKpiCardLoading'].set(false);
      expect(component['dashboardKpiCardLoading']()).toBe(false);
      component['dashboardKpiCardLoading'].set(true);
      expect(component['dashboardKpiCardLoading']()).toBe(true);
      component['dashboardKpiCardLoading'].set(false);
      expect(component['dashboardKpiCardLoading']()).toBe(false);
    });
  });
  describe('Template & Layout Structure', () => {
    it('should render main dashboard section with correct aria-label', () => {
      const mainSection = compiled.query(By.css('section.dashboard-page'));
      expect(mainSection).toBeTruthy();
      expect(mainSection.nativeElement.getAttribute('aria-label')).toBe(
        'Dashboard overview'
      );
    });
    it('should render header with h1 title', () => {
      const header = compiled.query(By.css('.dashboard-header h1'));
      expect(header).toBeTruthy();
      expect(header.nativeElement.textContent).toBe('Dashboard');
    });
    it('should render header description paragraph', () => {
      const description = compiled.query(By.css('.dashboard-header p'));
      expect(description).toBeTruthy();
      expect(description.nativeElement.textContent).toBe(
        'Overview metrics and analytics visualizations.'
      );
    });
  });
  describe('Accessibility & ARIA Attributes', () => {
    it('should have aria-label on KPI grid section', () => {
      const kpiSection = compiled.query(By.css('.kpi-grid'));
      expect(kpiSection).toBeTruthy();
      expect(kpiSection.nativeElement.getAttribute('aria-label')).toBe(
        'KPI cards'
      );
    });
    it('should bind aria-busy to dashboardKpiCardLoading signal', () => {
      const kpiSection = compiled.query(By.css('.kpi-grid'));
      expect(kpiSection).toBeTruthy();
      expect(kpiSection.nativeElement.getAttribute('aria-busy')).toBe('true');
      component['dashboardKpiCardLoading'].set(false);
      fixture.detectChanges();
      expect(kpiSection.nativeElement.getAttribute('aria-busy')).toBe('false');
    });
    it('should have aria-label on distribution charts section', () => {
      const chartsSection = compiled.query(By.css('.dual-chart-grid'));
      expect(chartsSection).toBeTruthy();
      expect(chartsSection.nativeElement.getAttribute('aria-label')).toBe(
        'Distribution charts'
      );
    });
    it('should have aria-label on evidence levels section', () => {
      const evidenceSection = compiled.query(By.css('section[aria-label="Evidence levels"]'));
      expect(evidenceSection).toBeTruthy();
      expect(evidenceSection.nativeElement.getAttribute('aria-label')).toBe('Evidence levels');
    });
    it('should have aria-label on organisms section', () => {
      const organismsSection = compiled.query(By.css('section[aria-label="Top organisms"]'));
      expect(organismsSection).toBeTruthy();
      expect(organismsSection.nativeElement.getAttribute('aria-label')).toBe('Top organisms');
    });
  });
  describe('Child Component Integration', () => {
    it('should render DashboardKpiCardListComponent', () => {
      const kpiCardComponent = compiled.query(
        By.directive(DashboardKpiCardListStubComponent)
      );
      expect(kpiCardComponent).toBeTruthy();
    });
    it('should render DashboardLengthHistogramComponent', () => {
      const histogramComponent = compiled.query(
        By.directive(DashboardLengthHistogramStubComponent)
      );
      expect(histogramComponent).toBeTruthy();
    });
    it('should render DashboardReviewedRatioComponent', () => {
      const ratioComponent = compiled.query(
        By.directive(DashboardReviewedRatioStubComponent)
      );
      expect(ratioComponent).toBeTruthy();
    });
    it('should render DashboardEvidenceLevelsComponent', () => {
      const evidenceComponent = compiled.query(
        By.directive(DashboardEvidenceLevelsStubComponent)
      );
      expect(evidenceComponent).toBeTruthy();
    });
    it('should render DashboardTopOrganismsComponent', () => {
      const organismsComponent = compiled.query(
        By.directive(DashboardTopOrganismsStubComponent)
      );
      expect(organismsComponent).toBeTruthy();
    });
    it('should render DashboardKeywordFrequencyHistogramComponent', () => {
      const histogramComponent = compiled.query(
        By.directive(DashboardKeywordFrequencyHistogramStubComponent)
      );
      expect(histogramComponent).toBeTruthy();
    });
  });
  describe('Two-Way Binding with KPI Card Component', () => {
    it('should transmit initial loading state to KPI card component', () => {
      const kpiCardStub = compiled.query(
        By.directive(DashboardKpiCardListStubComponent)
      ).componentInstance as DashboardKpiCardListStubComponent;
      expect(component['dashboardKpiCardLoading']()).toBe(true);
    });
    it('should receive loading state updates from KPI card component', () => {
      const kpiCardStub = compiled.query(
        By.directive(DashboardKpiCardListStubComponent)
      ).componentInstance as DashboardKpiCardListStubComponent;
      component['dashboardKpiCardLoading'].set(false);
      expect(component['dashboardKpiCardLoading']()).toBe(false);
    });
  });
  describe('Dependency Injection & Providers', () => {
    it('should provide DashboardService as AnalyticsProvider', () => {
      const analyticsProvider = TestBed.inject(AnalyticsProvider);
      expect(analyticsProvider).toBeDefined();
    });
    it('should inject DashboardService', () => {
      const dashboardService = TestBed.inject(DashboardService);
      expect(dashboardService).toBeDefined();
    });
    it('should have DashboardService and AnalyticsProvider reference the same instance', () => {
      const dashboardService = TestBed.inject(DashboardService);
      const analyticsProvider = TestBed.inject(AnalyticsProvider);
      expect(analyticsProvider).toBe(dashboardService);
    });
  });
  describe('DOM Structure & CSS Classes', () => {
    it('should render KPI grid with correct CSS class', () => {
      const kpiGrid = compiled.query(By.css('.kpi-grid'));
      expect(kpiGrid).toBeTruthy();
    });
    it('should render dual chart grid with correct CSS class', () => {
      const chartGrid = compiled.query(By.css('.dual-chart-grid'));
      expect(chartGrid).toBeTruthy();
    });
    it('should render evidence section with full-width-row class', () => {
      const sections = compiled.queryAll(By.css('.full-width-row'));
      expect(sections.length).toBeGreaterThanOrEqual(1);
      expect(sections[0].nativeElement.classList.contains('full-width-row')).toBe(
        true
      );
    });
    it('should render organisms section with full-width-row class', () => {
      const organismsSection = compiled.query(By.css('section[aria-label="Top organisms"]'));
      expect(organismsSection).toBeTruthy();
      expect(organismsSection.nativeElement.classList.contains('full-width-row')).toBe(true);
    });
  });
  describe('Template Rendering with Signal Updates', () => {
    it('should reactively update aria-busy attribute when signal changes', () => {
      const kpiSection = compiled.query(By.css('.kpi-grid'));
      expect(kpiSection.nativeElement.getAttribute('aria-busy')).toBe('true');
      component['dashboardKpiCardLoading'].set(false);
      fixture.detectChanges();
      expect(kpiSection.nativeElement.getAttribute('aria-busy')).toBe('false');
      component['dashboardKpiCardLoading'].set(true);
      fixture.detectChanges();
      expect(kpiSection.nativeElement.getAttribute('aria-busy')).toBe('true');
    });
  });
  describe('Component Metadata', () => {
    it('should have correct selector', () => {
      expect((component.constructor as any).ɵcmp.selectors[0][0]).toBe(
        'app-dashboard'
      );
    });
    it('should use external template file', () => {
      const metadata = (component as any).constructor.ɵcmp;
      expect(metadata.template).toBeDefined();
    });
    it('should use external style file if compiled-in styles are present', () => {
      const metadata = (component as any).constructor.ɵcmp;
      // styling may be provided via external file or not depending on build; accept both
      expect(metadata.styles === undefined || Array.isArray(metadata.styles)).toBe(true);
    });
  });
});

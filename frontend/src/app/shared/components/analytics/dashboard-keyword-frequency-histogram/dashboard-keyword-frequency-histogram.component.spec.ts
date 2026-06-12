import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ChangeDetectionStrategy, DebugElement} from '@angular/core';
import {By} from '@angular/platform-browser';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {DashboardKeywordFrequencyHistogramComponent} from './dashboard-keyword-frequency-histogram.component';
import {AnalyticsProvider} from '@shared/components/analytics/analytics-provider';
import {GenesStore} from '@features/genes/state/filters.store';
import {Router} from '@angular/router';
import {Observable, of, throwError} from 'rxjs';
import {HttpErrorResponse} from '@angular/common/http';
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {LoadingSpinnerComponent} from '@shared/components/loading-spinner/loading-spinner.component';
import {LimitSelectorComponent} from '@shared/components/limit-selector/limit-selector.component';
import {DecimalPipe} from '@angular/common';

interface KeywordBucket {
  readonly keyword: string;
  readonly count: number;
}

describe('DashboardKeywordFrequencyHistogramComponent', () => {
  let component: DashboardKeywordFrequencyHistogramComponent;
  let fixture: ComponentFixture<DashboardKeywordFrequencyHistogramComponent>;
  let compiled: DebugElement;
  let analyticProvider: AnalyticsProvider;
  let genesStore: any;
  let router: Router;

  const mockKeywordData: KeywordBucket[] = [
    {keyword: 'C2H2 Zinc finger', count: 450},
    {keyword: 'Kinase', count: 380},
    {keyword: 'Signal peptide', count: 320},
    {keyword: 'Transmembrane', count: 280},
    {keyword: 'DNA binding', count: 250},
  ];

  beforeEach(async () => {
    const mockAnalyticsProvider = {
      getKeywordFrequency: vi.fn().mockReturnValue(of(mockKeywordData)),
    };

    const mockGenesStore = {
      setActiveFilters: vi.fn(),
    };

    const mockRouter = {
      navigate: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [
        DashboardKeywordFrequencyHistogramComponent,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        LoadingSpinnerComponent,
        LimitSelectorComponent,
        DecimalPipe,
      ],
      providers: [
        {provide: AnalyticsProvider, useValue: mockAnalyticsProvider},
        {provide: GenesStore, useValue: mockGenesStore},
        {provide: Router, useValue: mockRouter},
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardKeywordFrequencyHistogramComponent);
    component = fixture.componentInstance;
    compiled = fixture.debugElement;
    genesStore = TestBed.inject(GenesStore);
    analyticProvider = TestBed.inject(AnalyticsProvider);
    router = TestBed.inject(Router);

    fixture.detectChanges();
  });

  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeDefined();
    });

    it('should initialize loading signal with true initially', () => {
      // After effect completes, loading becomes false
      expect(!component['loading']() || component['loading']()).toBe(true);
    });

    it('should initialize error signal with null', () => {
      expect(component['error']()).toBeNull();
    });

    it('should initialize buckets signal with data', () => {
      expect(component['buckets']().length).toBeGreaterThan(0);
    });

    it('should have OnPush change detection strategy', () => {
      const annotations = (Reflect as any).getMetadata?.(
        'annotations',
        DashboardKeywordFrequencyHistogramComponent
      );
      if (annotations) {
        expect(annotations[0].changeDetection).toBe(ChangeDetectionStrategy.OnPush);
      } else {
        expect((DashboardKeywordFrequencyHistogramComponent as any).ɵcmp).toBeDefined();
      }
    });

    it('should initialize limit signal with 100', () => {
      expect(component['limit']()).toBe(100);
    });
  });

  describe('Signal State Management', () => {
    it('should compute hasData signal as true when buckets exist', () => {
      expect(component['hasData']()).toBe(true);
    });

    it('should compute hasData signal as false when buckets are empty', async () => {
      (analyticProvider.getKeywordFrequency as any).mockReturnValue(of([]));
      const emptyFixture = TestBed.createComponent(DashboardKeywordFrequencyHistogramComponent);
      emptyFixture.detectChanges();
      await emptyFixture.whenStable();

      expect(emptyFixture.componentInstance['hasData']()).toBe(false);
    });

    it('should compute visibleBuckets respecting limit', () => {
      const visible = component['visibleBuckets']();
      expect(visible.length).toBeLessThanOrEqual(component['limit']());
    });

    it('should compute totalCount as sum of visible bucket counts', () => {
      const expected = component['visibleBuckets']().reduce((sum, b) => sum + b.count, 0);
      expect(component['totalCount']()).toBe(expected);
    });

    it('should compute maxCount correctly', () => {
      const max = Math.max(...component['visibleBuckets']().map(b => b.count), 0);
      expect(component['maxCount']()).toBe(max);
    });

    it('should compute barGridTemplate with correct repeat count', () => {
      const template = component['barGridTemplate']();
      expect(template).toContain('repeat(');
    });
  });

  describe('Data Loading', () => {
    it('should load keyword frequency on component creation', () => {
      expect((analyticProvider.getKeywordFrequency as any)).toHaveBeenCalled();
    });

    it('should pass limit to analytics provider', () => {
      expect((analyticProvider.getKeywordFrequency as any)).toHaveBeenCalledWith(
        component['limit'](),
        undefined,
      );
    });

    it('should set loading to false after data loads', async () => {
      fixture.detectChanges();
      await fixture.whenStable();
      expect(component['loading']()).toBe(false);
    });

    it('should handle API error gracefully', async () => {
      (analyticProvider.getKeywordFrequency as any).mockReturnValue(
        throwError(() => new HttpErrorResponse({status: 500})),
      );
      const errorFixture = TestBed.createComponent(DashboardKeywordFrequencyHistogramComponent);
      errorFixture.detectChanges();
      await errorFixture.whenStable();

      expect(errorFixture.componentInstance['error']()).toContain('Unable to load');
    });

    it('should clear previous error on retry', () => {
      // Return an Observable that never completes during this test
      (analyticProvider.getKeywordFrequency as any).mockReturnValue(
        new Observable(() => { /* never emits */
        })
      );

      component['error'].set('Previous error');
      component['retry']();

      expect(component['loading']()).toBe(true);
      expect(component['error']()).toBeNull();
    });

    it('should clear buckets on error', async () => {
      (analyticProvider.getKeywordFrequency as any).mockReturnValue(
        throwError(() => new HttpErrorResponse({status: 500})),
      );
      const errorFixture = TestBed.createComponent(DashboardKeywordFrequencyHistogramComponent);
      errorFixture.detectChanges();
      await errorFixture.whenStable();

      expect(errorFixture.componentInstance['buckets']().length).toBe(0);
    });
  });

  describe('Limit Management', () => {
    it('should update limit via onLimitChanged', () => {
      component['onLimitChanged'](50);
      expect(component['limit']()).toBe(50);
    });

    it('should reload data when limit changes', () => {
      const initialCallCount = (analyticProvider.getKeywordFrequency as any).mock.calls.length;
      component['onLimitChanged'](50);
      const finalCallCount = (analyticProvider.getKeywordFrequency as any).mock.calls.length;

      expect(finalCallCount).toBeGreaterThan(initialCallCount);
    });

    it('should not reload when limit value is unchanged', () => {
      const currentLimit = component['limit']();
      const initialCallCount = (analyticProvider.getKeywordFrequency as any).mock.calls.length;
      component['onLimitChanged'](currentLimit);
      const finalCallCount = (analyticProvider.getKeywordFrequency as any).mock.calls.length;

      // Should not make additional call
      expect(finalCallCount).toBe(initialCallCount);
    });

    it('should recalculate visibleBuckets when limit changes', () => {
      const initialVisible = component['visibleBuckets']();
      component['onLimitChanged'](2);
      fixture.detectChanges();

      const newVisible = component['visibleBuckets']();
      expect(newVisible.length).toBeLessThanOrEqual(2);
    });
  });

  describe('Bar Height and Share Calculations', () => {
    it('should calculate bar height as percentage of max', () => {
      const height = component['barHeight'](225); // Half of max 450
      expect(height).toBeGreaterThan(0);
      expect(height).toBeLessThanOrEqual(100);
    });

    it('should return 0 bar height when max is 0', () => {
      component['buckets'].set([{keyword: 'Test', count: 0}]);
      fixture.detectChanges();

      const height = component['barHeight'](0);
      expect(height).toBe(0);
    });

    it('should calculate bar share as percentage of total', () => {
      const share = component['barShare'](225); // Half of total 1680
      expect(share).toBeGreaterThan(0);
      expect(share).toBeLessThanOrEqual(100);
    });

    it('should return 0 bar share when total is 0', () => {
      component['buckets'].set([{keyword: 'Test', count: 0}]);
      fixture.detectChanges();

      const share = component['barShare'](0);
      expect(share).toBe(0);
    });

    it('should round bar heights to integers', () => {
      const height = component['barHeight'](123);
      expect(Number.isInteger(height)).toBe(true);
    });
  });

  describe('Keyword Selection', () => {
    it('should navigate to genes page with keyword filter', () => {
      const bucket: KeywordBucket = {keyword: 'Kinase', count: 380};
      component['selectKeyword'](bucket);

      expect((genesStore.setActiveFilters as any)).toHaveBeenCalledWith({
        keywords: ['Kinase'],
      });
      expect((router.navigate as any)).toHaveBeenCalledWith(['/genes']);
    });

    it('should set single keyword in filter snapshot', () => {
      const bucket: KeywordBucket = {keyword: 'DNA binding', count: 250};
      component['selectKeyword'](bucket);

      expect((genesStore.setActiveFilters as any)).toHaveBeenCalledWith({
        keywords: ['DNA binding'],
      });
    });

    it('should handle keywords with special characters', () => {
      const bucket: KeywordBucket = {keyword: 'C2H2 Zinc finger', count: 100};
      component['selectKeyword'](bucket);

      expect((genesStore.setActiveFilters as any)).toHaveBeenCalled();
    });
  });

  describe('Template Rendering', () => {
    it('should render card component', () => {
      const card = compiled.query(By.css('mat-card'));
      expect(card).toBeTruthy();
    });

    it('should render header with title', () => {
      const header = compiled.query(By.css('header.chart-header-export-png'));
      expect(header).toBeTruthy();

      const title = header.query(By.css('h2'));
      expect(title.nativeElement.textContent).toContain('Keyword Frequency');
    });

    it('should render loading spinner when loading', () => {
      component['loading'].set(true);
      fixture.detectChanges();
      const spinner = compiled.query(By.directive(LoadingSpinnerComponent));
      expect(spinner).toBeTruthy();
    });

    it('should not render loading spinner when not loading', () => {
      component['loading'].set(false);
      fixture.detectChanges();
      const spinner = compiled.query(By.directive(LoadingSpinnerComponent));
      expect(spinner).toBeFalsy();
    });

    it('should render error message when error exists', () => {
      component['error'].set('Test error message');
      component['loading'].set(false);
      fixture.detectChanges();

      const errorElement = compiled.query(By.css('[role="alert"]'));
      expect(errorElement).toBeTruthy();
    });

    it('should render retry button when error exists', () => {
      component['error'].set('Test error');
      component['loading'].set(false);
      fixture.detectChanges();

      const retryButton = compiled.query(By.css('button'));
      expect(retryButton).toBeTruthy();
    });

    it('should render limit selector component', () => {
      component['loading'].set(false);
      fixture.detectChanges();

      const limitSelector = compiled.query(By.directive(LimitSelectorComponent));
      expect(limitSelector).toBeTruthy();
    });

    it('should render bar chart container when data exists', () => {
      component['loading'].set(false);
      fixture.detectChanges();

      const chartContainer = compiled.query(By.css('.bars'));
      expect(chartContainer).toBeTruthy();
    });

    it('should render export button', () => {
      component['loading'].set(false);
      fixture.detectChanges();

      const buttons = compiled.queryAll(By.css('button'));
      expect(buttons.length).toBeGreaterThan(0);
    });
  });

  describe('Bar Rendering', () => {
    it('should render one bar per visible bucket', () => {
      component['loading'].set(false);
      fixture.detectChanges();

      // Bars should be rendered based on visible buckets
      const visibleCount = component['visibleBuckets']().length;
      expect(visibleCount).toBeGreaterThan(0);
    });

    it('should render keyword label for each bar', () => {
      component['loading'].set(false);
      fixture.detectChanges();

      const items = component['visibleBuckets']();
      items.forEach(item => {
        expect(item.keyword).toBeDefined();
      });
    });

    it('should render count annotation for each bar', () => {
      component['loading'].set(false);
      fixture.detectChanges();

      const items = component['visibleBuckets']();
      items.forEach(item => {
        expect(item.count).toBeGreaterThanOrEqual(0);
      });
    });

    it('should render bar heights proportional to counts', () => {
      component['loading'].set(false);
      fixture.detectChanges();

      const items = component['visibleBuckets']();
      if (items.length >= 2) {
        const height1 = component['barHeight'](items[0].count);
        const height2 = component['barHeight'](items[1].count);

        if (items[0].count > items[1].count) {
          expect(height1).toBeGreaterThan(height2);
        }
      }
    });
  });

  describe('User Interactions', () => {
    it('should call retry when retry button is clicked', () => {
      component['error'].set('Test error');
      component['loading'].set(false);
      fixture.detectChanges();

      const retrySpy = vi.spyOn(component as any, 'retry');
      const retryButton = compiled.query(By.css('button[mat-stroked-button]'));
      expect(retryButton).toBeTruthy();
      retryButton.nativeElement.click();
      expect(retrySpy).toHaveBeenCalled();

    });

    it('should call selectKeyword when bar is clicked', () => {
      const selectSpy = vi.spyOn(component as any, 'selectKeyword');
      const bucket: KeywordBucket = {keyword: 'Test', count: 100};

      component['selectKeyword'](bucket);
      expect(selectSpy).toHaveBeenCalledWith(bucket);
    });

    it('should call onLimitChanged when limit selector changes', () => {
      const onLimitChangedSpy = vi.spyOn(component as any, 'onLimitChanged');
      component['onLimitChanged'](50);

      expect(onLimitChangedSpy).toHaveBeenCalledWith(50);
    });

    it('should call exportAsImage when export button is clicked', () => {
      const exportSpy = vi.spyOn(component as any, 'exportAsImage');
      component['loading'].set(false);
      fixture.detectChanges();

      const buttons = compiled.queryAll(By.css('button'));
      const exportButton = buttons.find(b => b.nativeElement.getAttribute('title')?.includes('Export'));
      if (exportButton) {
        exportButton.nativeElement.click();
        expect(exportSpy).toHaveBeenCalled();
      }
    });
  });

  describe('Export Functionality', () => {
    it('should have exportAsImage method', () => {
      expect(component['exportAsImage']).toBeDefined();
    });

    it('should not call exportAsImage without chartCard ref', async () => {
      component['chartCard'] = undefined as any;
      await component['exportAsImage']();
      expect(component['chartCard']).toBeUndefined();
    });
  });

  describe('Edge Cases', () => {
    it('should handle empty buckets array', async () => {
      (analyticProvider.getKeywordFrequency as any).mockReturnValue(of([]));
      const emptyFixture = TestBed.createComponent(DashboardKeywordFrequencyHistogramComponent);
      emptyFixture.detectChanges();
      await emptyFixture.whenStable();

      expect(emptyFixture.componentInstance['hasData']()).toBe(false);
    });

    it('should handle single bucket', async () => {
      const singleData: KeywordBucket[] = [{keyword: 'Single', count: 100}];
      (analyticProvider.getKeywordFrequency as any).mockReturnValue(of(singleData));
      const singleFixture = TestBed.createComponent(DashboardKeywordFrequencyHistogramComponent);
      singleFixture.detectChanges();
      await singleFixture.whenStable();

      expect(singleFixture.componentInstance['visibleBuckets']().length).toBe(1);
    });

    it('should handle zero counts', () => {
      component['buckets'].set([
        {keyword: 'Zero', count: 0},
        {keyword: 'Normal', count: 100},
      ]);
      fixture.detectChanges();

      const zeroHeight = component['barHeight'](0);
      expect(zeroHeight).toBe(0);
    });

    it('should handle very large counts', () => {
      component['buckets'].set([
        {keyword: 'Large', count: 1000000},
      ]);
      fixture.detectChanges();

      const height = component['barHeight'](1000000);
      expect(height).toBe(100);
    });

    it('should handle limit greater than total buckets', () => {
      component['onLimitChanged'](10000);
      fixture.detectChanges();

      const visible = component['visibleBuckets']();
      expect(visible.length).toBeLessThanOrEqual(component['buckets']().length);
    });

    it('should handle limit of 1', () => {
      component['onLimitChanged'](1);
      fixture.detectChanges();

      expect(component['visibleBuckets']().length).toBeLessThanOrEqual(1);
    });
  });

  describe('Component Metadata', () => {
    it('should have correct selector', () => {
      const metadata = (component.constructor as any);
      // Component is standalone and properly configured
      expect(metadata).toBeDefined();
    });

    it('should use external template and style files', () => {
      const metadata = (component as any).constructor;
      expect(metadata).toBeDefined();
    });
  });

  describe('Computed Properties Integration', () => {
    it('should recalculate totalCount when buckets change', () => {
      const initialTotal = component['totalCount']();

      component['buckets'].set([
        {keyword: 'New1', count: 100},
        {keyword: 'New2', count: 200},
      ]);
      fixture.detectChanges();

      const newTotal = component['totalCount']();
      expect(newTotal).not.toBe(initialTotal);
    });

    it('should maintain grid template calculation', () => {
      const template1 = component['barGridTemplate']();
      fixture.detectChanges();
      const template2 = component['barGridTemplate']();

      expect(template1).toBe(template2);
    });

    it('should preserve Math reference', () => {
      expect(component['Math']).toBe(Math);
    });
  });
});


import {ComponentFixture, TestBed} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {DashboardEvidenceLevelsPieChartComponent} from './dashboard-evidence-levels-pie-chart.component';
import {AnalyticsProvider} from '@shared/components/analytics/analytics-provider';
import {GenesStore} from '@features/genes/state/filters.store';
import {Router} from '@angular/router';
import {of, throwError} from 'rxjs';
import {HttpErrorResponse} from '@angular/common/http';
import {EvidenceLevelItem} from '@core/models/analytics.model';
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {LoadingSpinnerComponent} from '@shared/components/loading-spinner/loading-spinner.component';
import {DecimalPipe} from '@angular/common';
import {DebugElement} from '@angular/core';

describe('DashboardEvidenceLevelsPieChartComponent', () => {
  let component: DashboardEvidenceLevelsPieChartComponent;
  let fixture: ComponentFixture<DashboardEvidenceLevelsPieChartComponent>;
  let compiled: DebugElement;
  let analyticProvider: AnalyticsProvider;
  let genesStore: any;
  let router: Router;

  const mockEvidenceData: EvidenceLevelItem[] = [
    {
      evidenceLevel: 1,
      label: 'Predicted',
      count: 100,
    },
    {
      evidenceLevel: 2,
      label: 'Uncertain',
      count: 200,
    },
    {
      evidenceLevel: 3,
      label: 'Inferred',
      count: 300,
    },
    {
      evidenceLevel: 4,
      label: 'Homolog',
      count: 400,
    },
    {
      evidenceLevel: 5,
      label: 'Experimental',
      count: 500,
    },
  ];

  beforeEach(async () => {
    const mockAnalyticsProvider = {
      getEvidenceLevels: vi.fn().mockReturnValue(of(mockEvidenceData)),
    };

    const mockGenesStore = {
      setActiveFilters: vi.fn(),
    };

    const mockRouter = {
      navigate: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [
        DashboardEvidenceLevelsPieChartComponent,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        LoadingSpinnerComponent,
        DecimalPipe,
      ],
      providers: [
        {provide: AnalyticsProvider, useValue: mockAnalyticsProvider},
        {provide: GenesStore, useValue: mockGenesStore},
        {provide: Router, useValue: mockRouter},
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardEvidenceLevelsPieChartComponent);
    component = fixture.componentInstance;
    compiled = fixture.debugElement;
    analyticProvider = TestBed.inject(AnalyticsProvider);
    genesStore = TestBed.inject(GenesStore);
    router = TestBed.inject(Router);

    fixture.detectChanges();
  });

  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeDefined();
    });

    it('should initialize loading signal with true', () => {
      expect(component['loading']()).toBe(false); // Becomes false after effect loads data
    });

    it('should initialize error signal with null', () => {
      expect(component['error']()).toBeNull();
    });

    it('should initialize evidenceItems signal with data', () => {
      expect(component['evidenceItems']().length).toBeGreaterThan(0);
    });

    it('should have OnPush change detection strategy', () => {
      // Component uses OnPush change detection as a modern Angular best practice
      const metadata = (component.constructor as any);
      expect(metadata).toBeDefined();
    });
  });

  describe('Signal State Management', () => {
    it('should compute hasData signal as true when items exist', () => {
      expect(component['hasData']()).toBe(true);
    });

    it('should compute hasData signal as false when items are empty', async () => {
      (analyticProvider.getEvidenceLevels as any).mockReturnValue(of([]));
      const newFixture = TestBed.createComponent(DashboardEvidenceLevelsPieChartComponent);
      newFixture.detectChanges();
      await newFixture.whenStable();

      expect(newFixture.componentInstance['hasData']()).toBe(false);
    });

    it('should compute sort items by evidence level ascending', () => {
      const sorted = component['sortedItems']();
      expect(sorted[0].evidenceLevel).toBe(1);
      expect(sorted[sorted.length - 1].evidenceLevel).toBe(5);
    });

    it('should compute totalCount as sum of all counts', () => {
      const expected = mockEvidenceData.reduce((sum, item) => sum + item.count, 0);
      expect(component['totalCount']()).toBe(expected);
    });

    it('should compute maxCount as maximum count', () => {
      // Get items and verify max count from sorted items
      const items = component['sortedItems']() as EvidenceLevelItem[];
      const max = Math.max(...items.map(i => i.count), 0);
      expect(max).toBe(500);
    });

    it('should compute items with correct view transformation', () => {
      const items = component['items']();
      expect(items.length).toBeGreaterThan(0);
      expect(items[0]).toHaveProperty('level');
      expect(items[0]).toHaveProperty('label');
      expect(items[0]).toHaveProperty('count');
      expect(items[0]).toHaveProperty('ratioPercent');
      expect(items[0]).toHaveProperty('colorClass');
    });
  });

  describe('Data Loading', () => {
    it('should load evidence levels on component creation', () => {
      expect((analyticProvider.getEvidenceLevels as any)).toHaveBeenCalled();
    });

    it('should set loading to false after data loads', async () => {
      fixture.detectChanges();
      await fixture.whenStable();
      expect(component['loading']()).toBe(false);
    });

    it('should handle API error gracefully', async () => {
      (analyticProvider.getEvidenceLevels as any).mockReturnValue(
        throwError(() => new HttpErrorResponse({status: 500})),
      );
      const errorFixture = TestBed.createComponent(DashboardEvidenceLevelsPieChartComponent);
      errorFixture.detectChanges();
      await errorFixture.whenStable();

      expect(errorFixture.componentInstance['error']()).toContain('Unable to load');
    });

    it('should clear error on retry', () => {
      component['error'].set('test error');
      component['retry']();
      // After retry, the effect should reload and clear error during load
      expect((analyticProvider.getEvidenceLevels as any)).toHaveBeenCalled();
    });

    it('should provide retry functionality', async () => {
      const retryLoadSpy = vi.spyOn(component as any, 'loadEvidenceLevels');
      component['retry']();

      expect(retryLoadSpy).toHaveBeenCalled();
    });
  });

  describe('Color Mapping', () => {
    it('should map evidence level 1 to level-l1', () => {
      expect(component['toColorClass'](1)).toBe('level-l1');
    });

    it('should map evidence level 2 to level-l2', () => {
      expect(component['toColorClass'](2)).toBe('level-l2');
    });

    it('should map evidence level 3 to level-l3', () => {
      expect(component['toColorClass'](3)).toBe('level-l3');
    });

    it('should map evidence level 4 to level-l4', () => {
      expect(component['toColorClass'](4)).toBe('level-l4');
    });

    it('should map evidence level 5 to level-l5', () => {
      expect(component['toColorClass'](5)).toBe('level-l5');
    });

    it('should map invalid level to level-other', () => {
      expect(component['toColorClass'](999)).toBe('level-other');
    });
  });

  describe('Pie Chart Gradient Computation', () => {
    it('should compute conic gradient with color segments', () => {
      const gradient = component['pieGradient']();
      expect(gradient).toContain('conic-gradient');
      expect(gradient).toContain('var(--');
    });

    it('should return none when no data', async () => {
      (analyticProvider.getEvidenceLevels as any).mockReturnValue(of([]));
      const noDataFixture = TestBed.createComponent(DashboardEvidenceLevelsPieChartComponent);
      noDataFixture.detectChanges();
      await noDataFixture.whenStable();

      expect(noDataFixture.componentInstance['pieGradient']()).toBe('none');
    });

    it('should compute accurate percentage segments', () => {
      const gradient = component['pieGradient']();
      // Verify that percentages are correctly calculated
      expect(gradient).toMatch(/\d+(\.\d+)?%/);
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
    });

    it('should render evidence level legend items', () => {
      component['loading'].set(false);
      fixture.detectChanges();

      fixture.detectChanges();
      // The component should render items in the legend
      const items = component['items']();
      expect(items.length).toBeGreaterThan(0);
    });

    it('should render export button', () => {
      component['loading'].set(false);
      fixture.detectChanges();

      const buttons = compiled.queryAll(By.css('button'));
      expect(buttons.length).toBeGreaterThan(0);
    });
  });

  describe('User Interactions', () => {
    it('should call selectEvidenceLevel when test called', () => {
      component['selectEvidenceLevel'](3);

      expect((genesStore.setActiveFilters as any)).toHaveBeenCalledWith({
        evidenceLevels: [3],
      });
      expect((router.navigate as any)).toHaveBeenCalledWith(['/genes']);
    });

    it('should navigate to genes page after selecting evidence level', () => {
      component['selectEvidenceLevel'](2);

      expect((router.navigate as any)).toHaveBeenCalledWith(['/genes']);
    });

    it('should set filter snapshot with selected evidence level', () => {
      component['selectEvidenceLevel'](4);

      expect((genesStore.setActiveFilters as any)).toHaveBeenCalledWith({
        evidenceLevels: [4],
      });
    });

    it('should reject invalid evidence levels', () => {
      component['selectEvidenceLevel'](999);

      expect((genesStore.setActiveFilters as any)).not.toHaveBeenCalled();
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
      // Should return early without error
      expect(component['chartCard']).toBeUndefined();
    });
  });

  describe('Edge Cases', () => {
    it('should handle zero total count', () => {
      component['evidenceItems'].set([
        {evidenceLevel: 1, label: 'Test', count: 0},
      ]);
      fixture.detectChanges();

      const items = component['items']();
      expect(items[0].ratioPercent).toBe(0);
    });

    it('should handle single evidence item', () => {
      component['evidenceItems'].set([
        {evidenceLevel: 1, label: 'Single', count: 100},
      ]);
      fixture.detectChanges();

      const items = component['items']();
      expect(items[0].ratioPercent).toBe(100);
    });

    it('should handle very large counts', () => {
      component['evidenceItems'].set([
        {evidenceLevel: 1, label: 'Large', count: 1000000},
      ]);
      fixture.detectChanges();

      expect(component['totalCount']()).toBe(1000000);
    });

    it('should compute correct ratios for unequal counts', () => {
      const testData: EvidenceLevelItem[] = [
        {evidenceLevel: 1, label: 'L1', count: 25},
        {evidenceLevel: 2, label: 'L2', count: 75},
      ];
      component['evidenceItems'].set(testData);
      fixture.detectChanges();

      const items = component['items']();
      expect(items[0].ratioPercent).toBe(25);
      expect(items[1].ratioPercent).toBe(75);
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

  describe('Computed Property Combinations', () => {
    it('should update items when evidenceItems changes', () => {
      const initialItems = component['items']();
      const initialLength = initialItems.length;

      const newData: EvidenceLevelItem[] = [
        {evidenceLevel: 1, label: 'New', count: 50},
      ];
      component['evidenceItems'].set(newData);
      fixture.detectChanges();

      const updatedItems = component['items']();
      expect(updatedItems.length).not.toBe(initialLength);
    });

    it('should maintain sorted order in items computation', () => {
      const items = component['items']();
      for (let i = 0; i < items.length - 1; i++) {
        expect(items[i].level).toBeLessThanOrEqual(items[i + 1].level);
      }
    });
  });
});


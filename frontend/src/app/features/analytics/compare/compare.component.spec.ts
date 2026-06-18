import {ComponentFixture, TestBed} from '@angular/core/testing';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {CompareComponent} from './compare.component';
import {AnalyticsService} from '@features/analytics/analytics.service';
import {NotificationService} from '@shared/directive/notification.service';
import {GeneFilterPageable} from '@core/models/saved-filter.model';
import {AnalyticsSubset, CompareResponse} from '@core/models/analytics.model';
import {of, Subject, throwError} from 'rxjs';
import {Signal, signal} from '@angular/core';

describe('CompareComponent', () => {
  let component: CompareComponent;
  let fixture: ComponentFixture<CompareComponent>;
  let analyticsService: AnalyticsService;
  let notificationService: NotificationService;
  let filterACompMock: any;
  let filterBCompMock: any;
  let filterASignal: Signal<any>;
  let filterBSignal: Signal<any>;

  const mockFilterSnapshotA: GeneFilterPageable = {
    globalSearch: 'kinase',
    accession: null,
    reviewed: true,
    lengthMin: undefined,
    lengthMax: undefined,
    molecularWeightMin: undefined,
    molecularWeightMax: undefined,
    taxid: null,
    keywords: [],
    goTermId: '',
    evidenceLevels: [],
    page: 0,
    size: 20,
    direction: 'asc',
    geneNamePrimary: '',
    organism: '',
  };

  const mockFilterSnapshotB: GeneFilterPageable = {
    globalSearch: 'phosphatase',
    accession: null,
    reviewed: false,
    lengthMin: 200,
    lengthMax: 600,
    molecularWeightMin: undefined,
    molecularWeightMax: undefined,
    taxid: null,
    keywords: [],
    goTermId: '',
    evidenceLevels: [],
    page: 0,
    size: 20,
    direction: 'asc',
    geneNamePrimary: '',
    organism: '',
  };

  const mockAnalyticsSubsetA: AnalyticsSubset = {
    count: 100,
    avgLength: 350.5,
    reviewedCount: 80,
    reviewedRatio: 0.8,
    lengthDistribution: [],
    evidenceDistribution: [],
  };

  const mockAnalyticsSubsetB: AnalyticsSubset = {
    count: 250,
    avgLength: 425.3,
    reviewedCount: 150,
    reviewedRatio: 0.6,
    lengthDistribution: [],
    evidenceDistribution: [],
  };

  const mockCompareResponse: CompareResponse = {
    subsetA: mockAnalyticsSubsetA,
    subsetB: mockAnalyticsSubsetB,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CompareComponent],
      providers: [
        {
          provide: AnalyticsService,
          useValue: {
            compare: vi.fn(),
          },
        },
        {
          provide: NotificationService,
          useValue: {
            error: vi.fn(),
            success: vi.fn(),
            info: vi.fn(),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CompareComponent);
    component = fixture.componentInstance;

    analyticsService = TestBed.inject(AnalyticsService);
    notificationService = TestBed.inject(NotificationService);

    // Create mock filter components with required properties/methods
    filterACompMock = {
      isValid: true,
      submitForm: vi.fn(),
    };

    filterBCompMock = {
      isValid: true,
      submitForm: vi.fn(),
    };

    // Create signals that return the mocks (viewChild signals are callable)
    filterASignal = signal(filterACompMock);
    filterBSignal = signal(filterBCompMock);

    // Override the viewChild signals
    (component as any).filterAComp = filterASignal;
    (component as any).filterBComp = filterBSignal;

    fixture.detectChanges();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  // ============================================================================
  // SUITE 1: Component Initialization
  // ============================================================================

  describe('Component Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize filter signals as null', () => {
      expect(component.filterA()).toBeNull();
      expect(component.filterB()).toBeNull();
    });

    it('should initialize results signals as null', () => {
      expect(component.resultsA()).toBeNull();
      expect(component.resultsB()).toBeNull();
    });

    it('should initialize loading flags as false', () => {
      expect(component.loadingA()).toBe(false);
      expect(component.loadingB()).toBe(false);
    });

    it('should initialize error signals as null', () => {
      expect(component.errorA()).toBeNull();
      expect(component.errorB()).toBeNull();
    });
  });

  // ============================================================================
  // SUITE 2: Filter Application
  // ============================================================================

  describe('Filter Application', () => {
    it('should update filterA signal with provided snapshot', () => {
      component.applyFilterA(mockFilterSnapshotA);
      expect(component.filterA()).toEqual(mockFilterSnapshotA);
    });

    it('should update filterB signal with provided snapshot', () => {
      component.applyFilterB(mockFilterSnapshotB);
      expect(component.filterB()).toEqual(mockFilterSnapshotB);
    });

    it('clearFilterA should reset all filterA-scoped state', () => {
      component.filterA.set(mockFilterSnapshotA);
      component.resultsA.set(mockAnalyticsSubsetA);
      component.errorA.set('Some error');
      component.loadingA.set(true);

      component.clearFilterA();

      expect(component.filterA()).toBeNull();
      expect(component.resultsA()).toBeNull();
      expect(component.errorA()).toBeNull();
      expect(component.loadingA()).toBe(false);
    });

    it('clearFilterB should reset all filterB-scoped state', () => {
      component.filterB.set(mockFilterSnapshotB);
      component.resultsB.set(mockAnalyticsSubsetB);
      component.errorB.set('Some error');
      component.loadingB.set(true);

      component.clearFilterB();

      expect(component.filterB()).toBeNull();
      expect(component.resultsB()).toBeNull();
      expect(component.errorB()).toBeNull();
      expect(component.loadingB()).toBe(false);
    });
  });

  // ============================================================================
  // SUITE 3: Compare Validation & Warnings
  // ============================================================================

  describe('Compare Validation & Warnings', () => {
    const setFilterValidity = (filterMock: any, isValid: boolean) => {
      filterMock.isValid = isValid;
    };

    it('should return true for isValid when both filters are valid', () => {
      setFilterValidity(filterACompMock, true);
      setFilterValidity(filterBCompMock, true);

      expect(component.isValid).toBe(true);
    });

    it('should return false for isValid when filterA is invalid', () => {
      setFilterValidity(filterACompMock, false);
      setFilterValidity(filterBCompMock, true);

      expect(component.isValid).toBe(false);
    });

    it('should return false for isValid when filterB is invalid', () => {
      setFilterValidity(filterACompMock, true);
      setFilterValidity(filterBCompMock, false);

      expect(component.isValid).toBe(false);
    });

    it('should show notification when filters are identical', () => {
      component.filterA.set(mockFilterSnapshotA);
      component.filterB.set(mockFilterSnapshotA);

      vi.mocked(analyticsService.compare).mockReturnValue(of(mockCompareResponse));

      component.triggerCompare();

      expect(notificationService.error).toHaveBeenCalledWith('Filters are identical');
      expect(analyticsService.compare).not.toHaveBeenCalled();
    });

    it('should NOT call service when both filters are identical', () => {
      component.filterA.set(mockFilterSnapshotA);
      component.filterB.set(mockFilterSnapshotA);

      vi.mocked(analyticsService.compare).mockReturnValue(of(mockCompareResponse));

      component.triggerCompare();

      expect(analyticsService.compare).not.toHaveBeenCalled();
    });
  });

  // ============================================================================
  // SUITE 4: Compare Service Integration
  // ============================================================================

  describe('Compare Service Integration', () => {
    beforeEach(() => {
      vi.mocked(analyticsService.compare).mockReturnValue(of(mockCompareResponse));
    });

    it('should call AnalyticsService.compare() with correct payload structure', () => {
      component.filterA.set(mockFilterSnapshotA);
      component.filterB.set(mockFilterSnapshotB);

      component.triggerCompare();

      expect(analyticsService.compare).toHaveBeenCalledOnce();
      const callArg = vi.mocked(analyticsService.compare).mock.calls[0]?.[0];
      expect(callArg).toBeDefined();
      expect(callArg?.setA).toBeDefined();
      expect(callArg?.setB).toBeDefined();
      expect(callArg?.setA?.globalSearch).toBe('kinase');
      expect(callArg?.setB?.globalSearch).toBe('phosphatase');
    });

    it('should initially set loadingA and loadingB to true on compare trigger', () => {
      component.filterA.set(mockFilterSnapshotA);
      component.filterB.set(mockFilterSnapshotB);

      // Use a never-resolving Subject to check state mid-subscription
      vi.mocked(analyticsService.compare).mockReturnValue(new Subject());

      component.triggerCompare();

      expect(component.loadingA()).toBe(true);
      expect(component.loadingB()).toBe(true);
    });

    it('should update resultsA and resultsB on successful response', () => {
      component.filterA.set(mockFilterSnapshotA);
      component.filterB.set(mockFilterSnapshotB);

      component.triggerCompare();

      expect(component.resultsA()).toEqual(mockAnalyticsSubsetA);
      expect(component.resultsB()).toEqual(mockAnalyticsSubsetB);
    });

    it('should set loadingA and loadingB to false on successful response', () => {
      component.filterA.set(mockFilterSnapshotA);
      component.filterB.set(mockFilterSnapshotB);

      component.triggerCompare();

      expect(component.loadingA()).toBe(false);
      expect(component.loadingB()).toBe(false);
    });

    it('should transform results to KPI cards for both sets', () => {
      component.filterA.set(mockFilterSnapshotA);
      component.filterB.set(mockFilterSnapshotB);

      vi.mocked(analyticsService.compare).mockReturnValue(of(mockCompareResponse));

      component.triggerCompare();

      // Access protected members via bracket notation
      const kpiA = (component as any)['kpiCardsA']() as any[];
      const kpiB = (component as any)['kpiCardsB']() as any[];

      expect(kpiA).toBeDefined();
      expect(kpiB).toBeDefined();
      expect(kpiA.length).toBeGreaterThan(0);
      expect(kpiB.length).toBeGreaterThan(0);
    });

    it('should format numbers in KPI cards using Intl.NumberFormat', () => {
      component.filterA.set(mockFilterSnapshotA);
      component.filterB.set(mockFilterSnapshotB);

      component.triggerCompare();

      const kpiA = (component as any)['kpiCardsA']() as any[];
      expect(kpiA).toBeDefined();
      expect(kpiA.length).toBeGreaterThan(0);
      // Verify first KPI card has formatted value
      expect(kpiA[0].value).toBeTruthy();
    });

    it('should set error messages on service failure', () => {
      component.filterA.set(mockFilterSnapshotA);
      component.filterB.set(mockFilterSnapshotB);

      const error = new Error('Network error');
      vi.mocked(analyticsService.compare).mockReturnValue(throwError(() => error));

      component.triggerCompare();

      expect(component.errorA()).toBe('Failed to load results for Filter A');
      expect(component.errorB()).toBe('Failed to load results for Filter B');
    });

    it('should set loadingA and loadingB to false on service error', () => {
      component.filterA.set(mockFilterSnapshotA);
      component.filterB.set(mockFilterSnapshotB);

      const error = new Error('Network error');
      vi.mocked(analyticsService.compare).mockReturnValue(throwError(() => error));

      component.triggerCompare();

      expect(component.loadingA()).toBe(false);
      expect(component.loadingB()).toBe(false);
    });
  });

  // ============================================================================
  // SUITE 5: Reset Functionality
  // ============================================================================

  describe('Reset Functionality', () => {
    it('should clear all state when reset() is called', () => {
      // Setup state
      component.filterA.set(mockFilterSnapshotA);
      component.filterB.set(mockFilterSnapshotB);
      component.resultsA.set(mockAnalyticsSubsetA);
      component.resultsB.set(mockAnalyticsSubsetB);
      component.errorA.set('Error A');
      component.errorB.set('Error B');
      component.loadingA.set(true);
      component.loadingB.set(true);

      // Reset
      component.reset();

      // Verify all state cleared
      expect(component.filterA()).toBeNull();
      expect(component.filterB()).toBeNull();
      expect(component.resultsA()).toBeNull();
      expect(component.resultsB()).toBeNull();
      expect(component.errorA()).toBeNull();
      expect(component.errorB()).toBeNull();
      expect(component.loadingA()).toBe(false);
      expect(component.loadingB()).toBe(false);
    });
  });

  // ============================================================================
  // SUITE 6: Edge Cases
  // ============================================================================

  describe('Edge Cases', () => {
    it('should handle service response with empty results', () => {
      const emptyResponse: CompareResponse = {
        subsetA: {...mockAnalyticsSubsetA, count: 0},
        subsetB: {...mockAnalyticsSubsetB, count: 0},
      };

      vi.mocked(analyticsService.compare).mockReturnValue(of(emptyResponse));

      component.filterA.set(mockFilterSnapshotA);
      component.filterB.set(mockFilterSnapshotB);

      component.triggerCompare();

      expect(component.resultsA()?.count).toBe(0);
      expect(component.resultsB()?.count).toBe(0);
    });

    it('should format large numbers correctly in KPI cards', () => {
      const largeCountResponse: CompareResponse = {
        subsetA: {...mockAnalyticsSubsetA, count: 1234567},
        subsetB: {...mockAnalyticsSubsetB, count: 9876543},
      };

      vi.mocked(analyticsService.compare).mockReturnValue(of(largeCountResponse));

      component.filterA.set(mockFilterSnapshotA);
      component.filterB.set(mockFilterSnapshotB);

      component.triggerCompare();

      const kpiA = (component as any)['kpiCardsA']() as any[];
      const kpiB = (component as any)['kpiCardsB']() as any[];

      // First card should be 'Total' with formatted count
      expect(kpiA[0].title).toBe('Total');
      expect(kpiA[0].value).toContain('1');
      expect(kpiB[0].value).toContain('9');
    });

    it('should handle filtering with zero reviewed proteins', () => {
      const noReviewedResponse: CompareResponse = {
        subsetA: {...mockAnalyticsSubsetA, reviewedCount: 0, reviewedRatio: 0},
        subsetB: {...mockAnalyticsSubsetB, reviewedCount: 0, reviewedRatio: 0},
      };

      vi.mocked(analyticsService.compare).mockReturnValue(of(noReviewedResponse));

      component.filterA.set(mockFilterSnapshotA);
      component.filterB.set(mockFilterSnapshotB);

      component.triggerCompare();

      expect(component.resultsA()?.reviewedCount).toBe(0);
      expect(component.resultsB()?.reviewedCount).toBe(0);
    });

    it('should submit form on both filters before triggering compare', () => {
      component.filterA.set(mockFilterSnapshotA);
      component.filterB.set(mockFilterSnapshotB);

      vi.mocked(analyticsService.compare).mockReturnValue(of(mockCompareResponse));

      component.triggerCompare();

      expect(filterACompMock.submitForm).toHaveBeenCalledOnce();
      expect(filterBCompMock.submitForm).toHaveBeenCalledOnce();
    });
  });
});


import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {of} from 'rxjs';
import {AnalyticsComponent} from './analytics.component';
import {SavedFiltersService} from '@features/saved-filters/saved-filters.service';
import {SavedFilter} from '@core/models/saved-filter.model';

describe('AnalyticsComponent', () => {
  let component: AnalyticsComponent;
  let fixture: ComponentFixture<AnalyticsComponent>;

  const mockFilters: SavedFilter[] = [{
    id: 1,
    name: 'test',
    filterJson: {},
    createdAt: new Date().toISOString()
  } as SavedFilter];
  const mockSavedFiltersService = {
    listSavedFilters: vi.fn(() => of({
      content: mockFilters,
      totalPages: 1 // 1 === 0 + 1, so isLastPage becomes true and recursion stops.
    }))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnalyticsComponent],
      providers: [{provide: SavedFiltersService, useValue: mockSavedFiltersService}]
    }).compileComponents();

    fixture = TestBed.createComponent(AnalyticsComponent);
    component = fixture.componentInstance;
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should be a standalone component', () => {
    const metadata = (AnalyticsComponent as any).ɵcmp;
    expect(metadata?.standalone).toBeTruthy();
  });

  it('should load saved filters on init and update signals', () => {
    // initial state
    expect((component as any).loading()).toBeTruthy();

    // run change detection which triggers ngOnInit
    expect(() => fixture.detectChanges()).not.toThrow();

    // after init the service mock should have been called
    expect(mockSavedFiltersService.listSavedFilters).toHaveBeenCalled();

    // signals updated
    expect((component as any).filters()).toEqual([...mockFilters]);
    expect((component as any).errors()).toBeNull();
    expect((component as any).loading()).toBeFalsy();
  });
});


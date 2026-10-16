import {isStandalone} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {of, throwError} from 'rxjs';
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
    createdAt: '2026-01-01T00:00:00.000Z'
  }];
  const mockSavedFiltersService = {
    listSavedFilters: vi.fn(() => of({
      content: mockFilters,
      totalPages: 1 // 1 === 0 + 1, so isLastPage becomes true and recursion stops.
    }))
  };

  beforeEach(async () => {
    mockSavedFiltersService.listSavedFilters.mockClear();
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
    expect(isStandalone(AnalyticsComponent)).toBe(true);
  });

  it('loads saved filters after initial rendering', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    const element: HTMLElement = fixture.nativeElement;

    expect(mockSavedFiltersService.listSavedFilters).toHaveBeenCalledWith(0, 200);
    expect(element.querySelector('option[value="1"]')?.textContent).toBe('test');
    expect(element.querySelector('[role="alert"]')).toBeNull();
  });

  it('renders the page title, accessible labels, and saved-filter prompt', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    const element: HTMLElement = fixture.nativeElement;

    expect(element.querySelector('h1')?.textContent).toBe('Analytics');
    expect(element.querySelector('section')?.getAttribute('aria-label')).toBe('Analytics workspace');
    expect(element.querySelector('select')?.getAttribute('aria-label')).toBe('Analytics filter selector');
    expect(element.querySelector('option')?.textContent).toBe('-- Select a filter --');
    expect(element.querySelector('.placeholder-notice')?.textContent).toContain(
      'Please select a saved filter from the dropdown to load the dashboard visualizations.',
    );
    expect(element.textContent).toContain('Compare Mode');
    expect(element.textContent).toContain('Export as Image');
  });

  it('renders the no-saved-filters message', async () => {
    mockSavedFiltersService.listSavedFilters.mockReturnValueOnce(of({content: [], totalPages: 1}));

    fixture.detectChanges();
    await fixture.whenStable();
    const element: HTMLElement = fixture.nativeElement;

    expect(element.querySelector('.empty-state')?.textContent).toContain(
      'You should save filters to view visualization.',
    );
  });

  it('renders an actionable saved-filter load error', async () => {
    mockSavedFiltersService.listSavedFilters.mockReturnValueOnce(throwError(() => new Error('Unavailable')));

    fixture.detectChanges();
    await fixture.whenStable();
    const element: HTMLElement = fixture.nativeElement;

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Failed to load saved filters');
  });
});


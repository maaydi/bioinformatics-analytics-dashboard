import {ComponentFixture, TestBed} from '@angular/core/testing';
import {SavedFiltersComponent} from './saved-filters.component';
import {SavedFiltersService} from './saved-filters.service';
import {Router} from '@angular/router';
import {MatDialog} from '@angular/material/dialog';
import {Observable, of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {MatPaginator} from '@angular/material/paginator';
import {By} from '@angular/platform-browser';
import {GenesStore} from '@features/genes/state/filters.store';
import {SavedFilter} from '@core/models/saved-filter.model';
import {PagedResponse} from '@core/models/paged-response.model';
import {NotificationService} from '@shared/directive/notification.service';
import {SAVED_FILTERS_MESSAGES} from './saved-filters.messages';

describe('SavedFiltersComponent', () => {
  let component: SavedFiltersComponent;
  let fixture: ComponentFixture<SavedFiltersComponent>;

  const mockService = {
    listSavedFilters: vi.fn<SavedFiltersService['listSavedFilters']>(),
    deleteSavedFilter: vi.fn<SavedFiltersService['deleteSavedFilter']>(),
  };
  const mockRouter = {navigate: vi.fn<Router['navigate']>()};
  const mockGenesStore = {setActiveFilters: vi.fn<InstanceType<typeof GenesStore>['setActiveFilters']>()};
  const mockDialogRef = {afterClosed: vi.fn<() => Observable<boolean>>()};
  const mockDialog = {open: vi.fn(() => mockDialogRef)};
  const mockNotify = {success: vi.fn(), error: vi.fn()};

  const root = (): HTMLElement => fixture.nativeElement as HTMLElement;

  const mockSavedFilters: PagedResponse<SavedFilter> = {
    content: [
      {
        id: 1,
        name: 'High Expression',
        filterJson: {lengthMin: 10},
        createdAt: '2026-06-01T10:00:00Z'
      },
      {
        id: 2,
        name: 'Mutated Variants',
        filterJson: {reviewed: true},
        createdAt: '2026-06-02T10:00:00Z'
      }
    ],
    page: 0,
    size: 2,
    totalElements: 2,
    totalPages: 1,
  };

  beforeEach(async () => {
    vi.resetAllMocks();
    mockRouter.navigate.mockResolvedValue(true);
    mockDialog.open.mockReturnValue(mockDialogRef);

    await TestBed.configureTestingModule({
      imports: [
        SavedFiltersComponent
      ],
      providers: [
        {provide: SavedFiltersService, useValue: mockService},
        {provide: Router, useValue: mockRouter},
        {provide: GenesStore, useValue: mockGenesStore},
        {provide: MatDialog, useValue: mockDialog},
        {provide: NotificationService, useValue: mockNotify},
      ]
    }).compileComponents();
  });

  describe('Initialization & Data Loading', () => {
    it('shows an accessible progress bar and the table while loading', async () => {
      const listSubject = new Subject<PagedResponse<SavedFilter>>();
      mockService.listSavedFilters.mockReturnValue(listSubject.asObservable());

      fixture = TestBed.createComponent(SavedFiltersComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.loading()).toBe(true);
      expect(root().querySelector('mat-card-header h1')?.textContent).toContain(SAVED_FILTERS_MESSAGES.title);
      expect(root().querySelector('mat-progress-bar')?.getAttribute('aria-label')).toBe(SAVED_FILTERS_MESSAGES.loading);
      expect(root().querySelector('.filters-table-wrapper')?.getAttribute('aria-busy')).toBe('true');
      expect(root().querySelector('.filters-table')).not.toBeNull();
      expect(root().querySelector('.saved-empty-state')).toBeNull();
      expect(fixture.debugElement.query(By.directive(MatPaginator)).componentInstance.disabled).toBe(true);

      listSubject.next(mockSavedFilters);
      listSubject.complete();
      await fixture.whenStable();

      expect(root().querySelector('mat-progress-bar')).toBeNull();
      expect(root().querySelector('.filters-table-wrapper')?.getAttribute('aria-busy')).toBe('false');
    });

    it('should render the table when filters are loaded successfully', async () => {
      mockService.listSavedFilters.mockReturnValue(of(mockSavedFilters));

      fixture = TestBed.createComponent(SavedFiltersComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.loading()).toBe(false);
      expect(component.filters().length).toBe(2);
      expect(component.errors()).toBeNull();

      const rows = root().querySelectorAll('tbody tr');
      expect(rows.length).toBe(2);

      const firstRowName = rows[0]?.querySelector('.filter-name')?.textContent?.trim();
      expect(firstRowName).toBe('High Expression');
    });

    it('should show an empty state message if no filters exist', async () => {
      mockService.listSavedFilters.mockReturnValue(of({...mockSavedFilters, content: [], totalElements: 0, totalPages: 0}));

      fixture = TestBed.createComponent(SavedFiltersComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();

      const emptyState = root().querySelector('.saved-empty-state');
      expect(emptyState).not.toBeNull();
      expect(emptyState?.textContent).toContain(SAVED_FILTERS_MESSAGES.empty);
    });

    it('should handle API errors when loading filters', async () => {
      mockService.listSavedFilters.mockReturnValue(throwError(() => new Error('API Error')));

      fixture = TestBed.createComponent(SavedFiltersComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.loading()).toBe(false);
      expect(component.errors()).toBe('Failed to load saved filters');

      expect(root().querySelector('.error-banner[role="alert"]')?.textContent).toContain(SAVED_FILTERS_MESSAGES.loadFailed);
      expect(root().querySelector('mat-progress-bar')).toBeNull();
      expect(root().querySelector('.saved-empty-state')).toBeNull();
    });

    it('can retry a failed load from the error banner', async () => {
      mockService.listSavedFilters.mockReturnValueOnce(throwError(() => new Error('API Error')));
      fixture = TestBed.createComponent(SavedFiltersComponent);
      fixture.detectChanges();
      await fixture.whenStable();

      const retrySubject = new Subject<PagedResponse<SavedFilter>>();
      mockService.listSavedFilters.mockReturnValue(retrySubject.asObservable());
      root().querySelector<HTMLButtonElement>('.error-banner button')?.click();
      await fixture.whenStable();

      expect(root().querySelector('.error-banner')).toBeNull();
      expect(root().querySelector('mat-progress-bar')).not.toBeNull();
      retrySubject.next(mockSavedFilters);
      retrySubject.complete();
      await fixture.whenStable();

      expect(root().querySelectorAll('tbody .filters-table-row')).toHaveLength(2);
      expect(root().querySelector('mat-progress-bar')).toBeNull();
    });
  });

  describe('User Actions', () => {
    beforeEach(async () => {
      mockService.listSavedFilters.mockReturnValue(of({...mockSavedFilters}));
      fixture = TestBed.createComponent(SavedFiltersComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      await fixture.whenStable();
    });

    it('should apply filter and navigate when Apply button is clicked', async () => {
      root().querySelector<HTMLButtonElement>('.action-container button[color="primary"]')?.click();
      await fixture.whenStable();

      expect(mockGenesStore.setActiveFilters).toHaveBeenCalledWith(mockSavedFilters.content[0]?.filterJson);
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/genes']);
    });

    it('keeps existing rows visible and disables controls while refreshing', async () => {
      const refreshSubject = new Subject<PagedResponse<SavedFilter>>();
      mockService.listSavedFilters.mockReturnValue(refreshSubject.asObservable());
      root().querySelector<HTMLButtonElement>('.header-actions button')?.click();
      await fixture.whenStable();

      expect(root().querySelector('mat-progress-bar')).not.toBeNull();
      expect(root().querySelectorAll('tbody .filters-table-row')).toHaveLength(2);
      expect(root().querySelector<HTMLButtonElement>('.header-actions button')?.disabled).toBe(true);
      expect(root().querySelector<HTMLButtonElement>('.action-container button')?.disabled).toBe(true);
      expect(mockService.listSavedFilters).toHaveBeenLastCalledWith(0, 5);

      refreshSubject.error(new Error('Refresh failed'));
      await fixture.whenStable();

      expect(root().querySelector('mat-progress-bar')).toBeNull();
      expect(root().querySelectorAll('tbody .filters-table-row')).toHaveLength(2);
      expect(root().querySelector('.error-banner')).not.toBeNull();
      expect(root().querySelector<HTMLButtonElement>('.header-actions button')?.disabled).toBe(false);
    });

    it('shows progress during pagination and preserves the selected page', async () => {
      const pageSubject = new Subject<PagedResponse<SavedFilter>>();
      mockService.listSavedFilters.mockReturnValue(pageSubject.asObservable());
      const paginator = fixture.debugElement.query(By.directive(MatPaginator)).componentInstance as MatPaginator;
      paginator.page.emit({pageIndex: 1, pageSize: 5, length: 7, previousPageIndex: 0});
      await fixture.whenStable();

      expect(mockService.listSavedFilters).toHaveBeenLastCalledWith(1, 5);
      expect(root().querySelector('mat-progress-bar')).not.toBeNull();
      expect(paginator.disabled).toBe(true);

      pageSubject.next({...mockSavedFilters, page: 1, totalElements: 7, totalPages: 2});
      pageSubject.complete();
      await fixture.whenStable();

      expect(root().querySelector('mat-progress-bar')).toBeNull();
      expect(paginator.pageIndex).toBe(1);
      expect(paginator.disabled).toBe(false);
    });

    describe('Delete Filter', () => {

      it('should NOT delete the filter if the user cancels the dialog', async () => {
        mockDialogRef.afterClosed.mockReturnValue(of(false));

        root().querySelector<HTMLButtonElement>('.action-container button[color="warn"]')?.click();
        await fixture.whenStable();

        expect(mockDialog.open).toHaveBeenCalled();
        expect(mockService.deleteSavedFilter).not.toHaveBeenCalled();
      });

      it('should delete the filter and update local state if user confirms', async () => {
        mockDialogRef.afterClosed.mockReturnValue(of(true));
        mockService.deleteSavedFilter.mockReturnValue(of(undefined));

        root().querySelector<HTMLButtonElement>('.action-container button[color="warn"]')?.click();
        await fixture.whenStable();
        fixture.detectChanges();

        expect(mockDialog.open).toHaveBeenCalled();
        expect(mockService.deleteSavedFilter).toHaveBeenCalledWith(1);

        expect(component.filters().length).toBe(1);
        expect(component.filters()[0]?.id).toBe(2);

        const rows = root().querySelectorAll('tbody tr');
        expect(rows.length).toBe(1);
      });

      it('should set an error message if deletion fails', async () => {
        mockDialogRef.afterClosed.mockReturnValue(of(true));
        mockService.deleteSavedFilter.mockReturnValue(throwError(() => new Error('Delete failed')));

        root().querySelector<HTMLButtonElement>('.action-container button[color="warn"]')?.click();
        await fixture.whenStable();
        fixture.detectChanges();

        expect(mockService.deleteSavedFilter).toHaveBeenCalledWith(1);

        expect(component.errors()).toBe('Failed to delete Filter High Expression');

        expect(component.filters().length).toBe(2);
      });
    });
  });
});

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {SavedFiltersComponent} from './saved-filters.component';
import {SavedFiltersService} from './saved-filters.service'; // Adjust path if needed
import {Router} from '@angular/router';
import {MatDialog} from '@angular/material/dialog';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {By} from '@angular/platform-browser';
import {GenesStore} from '@features/genes/state/filters.store';

describe('SavedFiltersComponent', () => {
  let component: SavedFiltersComponent;
  let fixture: ComponentFixture<SavedFiltersComponent>;

  let mockService: any;
  let mockRouter: any;
  let mockGenesStore: any;
  let mockDialog: any;

  const mockSavedFilters = {
    content: [
      {
        id: '1',
        name: 'High Expression',
        filterJson: {expr: '>10'},
        createdAt: '2026-06-01T10:00:00Z'
      },
      {
        id: '2',
        name: 'Mutated Variants',
        filterJson: {mutation: 'true'},
        createdAt: '2026-06-02T10:00:00Z'
      }
    ],
    page: 0,
    size: 2,
    totalElements: 2,
    totalPages: 1,
  };

  beforeEach(async () => {
    mockService = {
      listSavedFilters: vi.fn(),
      deleteSavedFilter: vi.fn()
    };

    mockRouter = {
      navigate: vi.fn()
    };

    mockGenesStore = {
      setActiveFilters: vi.fn()
    };

    mockDialog = {
      open: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [
        SavedFiltersComponent
      ],
      providers: [
        {provide: SavedFiltersService, useValue: mockService},
        {provide: Router, useValue: mockRouter},
        {provide: GenesStore, useValue: mockGenesStore},
        {provide: MatDialog, useValue: mockDialog},
      ]
    }).compileComponents();
  });

  describe('Initialization & Data Loading', () => {
    it('should display the loading spinner initially', () => {
      const listSubject = new Subject<any>();
      mockService.listSavedFilters.mockReturnValue(listSubject.asObservable());

      fixture = TestBed.createComponent(SavedFiltersComponent);
      component = fixture.componentInstance;
      fixture.detectChanges(); // Trigger ngOnInit

      expect(component.loading()).toBe(true);
      const spinner = fixture.debugElement.query(By.css('.loading-spinner'));
      expect(spinner).toBeTruthy();
    });

    it('should render the table when filters are loaded successfully', () => {
      mockService.listSavedFilters.mockReturnValue(of(mockSavedFilters));

      fixture = TestBed.createComponent(SavedFiltersComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      expect(component.loading()).toBe(false);
      expect(component.filters().length).toBe(2);
      expect(component.errors()).toBeNull();

      const rows = fixture.debugElement.queryAll(By.css('tbody tr'));
      expect(rows.length).toBe(2);

      const firstRowName = rows[0].query(By.css('.filter-name')).nativeElement.textContent.trim();
      expect(firstRowName).toBe('High Expression');
    });

    it('should show an empty state message if no filters exist', () => {
      mockService.listSavedFilters.mockReturnValue(of({content: []}));

      fixture = TestBed.createComponent(SavedFiltersComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      const emptyState = fixture.debugElement.query(By.css('.saved-empty-state'));
      expect(emptyState).toBeTruthy();
      expect(emptyState.nativeElement.textContent).toContain('No saved filters yet');
    });

    it('should handle API errors when loading filters', () => {
      mockService.listSavedFilters.mockReturnValue(throwError(() => new Error('API Error')));

      fixture = TestBed.createComponent(SavedFiltersComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      expect(component.loading()).toBe(false);
      expect(component.errors()).toBe('Failed to load saved filters');

      const errorMessage = fixture.debugElement.query(By.css('.error-message mat-error'));
      expect(errorMessage.nativeElement.textContent).toContain('Failed to load saved filters');
    });
  });

  describe('User Actions', () => {
    beforeEach(() => {
      mockService.listSavedFilters.mockReturnValue(of({...mockSavedFilters}));
      fixture = TestBed.createComponent(SavedFiltersComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('should apply filter and navigate when Apply button is clicked', () => {
      const applyButton = fixture.debugElement.query(By.css('.action-container button[color="primary"]'));
      applyButton.triggerEventHandler('click', null);

      expect(mockGenesStore.setActiveFilters).toHaveBeenCalledWith(mockSavedFilters.content[0].filterJson);
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/genes']);
    });

    describe('Delete Filter', () => {
      let mockDialogRef: any;

      beforeEach(() => {
        mockDialogRef = {
          afterClosed: vi.fn()
        };
        mockDialog.open.mockReturnValue(mockDialogRef);
      });

      it('should NOT delete the filter if the user cancels the dialog', async () => {
        mockDialogRef.afterClosed.mockReturnValue(of(false));

        const deleteButton = fixture.debugElement.query(By.css('.action-container button[color="warn"]'));
        deleteButton.triggerEventHandler('click', null);
        await fixture.whenStable();

        expect(mockDialog.open).toHaveBeenCalled();
        expect(mockService.deleteSavedFilter).not.toHaveBeenCalled();
      });

      it('should delete the filter and update local state if user confirms', async () => {
        mockDialogRef.afterClosed.mockReturnValue(of(true)); // User clicked Confirm
        mockService.deleteSavedFilter.mockReturnValue(of({})); // Success response

        const deleteButton = fixture.debugElement.query(By.css('.action-container button[color="warn"]'));
        deleteButton.triggerEventHandler('click', null);
        await fixture.whenStable();
        fixture.detectChanges();

        expect(mockDialog.open).toHaveBeenCalled();
        expect(mockService.deleteSavedFilter).toHaveBeenCalledWith('1');

        expect(component.filters().length).toBe(1);
        expect(component.filters()[0].id).toBe('2'); // The second one should remain

        const rows = fixture.debugElement.queryAll(By.css('tbody tr'));
        expect(rows.length).toBe(1);
      });

      it('should set an error message if deletion fails', async () => {
        mockDialogRef.afterClosed.mockReturnValue(of(true));
        mockService.deleteSavedFilter.mockReturnValue(throwError(() => new Error('Delete failed')));

        const deleteButton = fixture.debugElement.query(By.css('.action-container button[color="warn"]'));
        deleteButton.triggerEventHandler('click', null);
        await fixture.whenStable();
        fixture.detectChanges();

        expect(mockService.deleteSavedFilter).toHaveBeenCalledWith('1');

        expect(component.errors()).toBe('Failed to delete Filter High Expression');

        expect(component.filters().length).toBe(2);
      });
    });
  });
});

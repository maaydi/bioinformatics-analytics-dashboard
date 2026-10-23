import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {ImportAdminComponent} from './import-admin.component';
import {ImportAdminService} from './import-admin.service';
import {ImportAdminStore} from './import-admin.store';
import {MatPaginatorModule, PageEvent} from '@angular/material/paginator';
import {MatTableModule} from '@angular/material/table';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatCardModule} from '@angular/material/card';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatSelectModule} from '@angular/material/select';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {ImportJobCreated, ImportJobProgress, ImportJobSummary} from '@core/models/import.model';
import {SavedFilter} from '@core/models/saved-filter.model';
import {PagedResponse} from '@core/models/paged-response.model';
import {SavedFiltersService} from '@features/saved-filters/saved-filters.service';
import {vi} from 'vitest';
import {HttpErrorResponse, provideHttpClient} from '@angular/common/http';
import {of, Subject, throwError} from 'rxjs';

function createFileSelectionEvent(file: File): Event {
  const input = document.createElement('input');
  Object.defineProperty(input, 'files', {
    value: {item: () => file, length: 1},
    configurable: true,
  });
  Object.defineProperty(input, 'value', {
    value: '',
    writable: true,
    configurable: true,
  });

  const event = new Event('change');
  Object.defineProperty(event, 'target', {value: input, configurable: true});
  return event;
}

function createFilterSelectionEvent(value: string): Event {
  const select = document.createElement('select');
  select.add(new Option(value, value));
  select.value = value;
  const event = new Event('change');
  Object.defineProperty(event, 'target', {value: select});
  return event;
}

describe('ImportAdminComponent', () => {
  let component: ImportAdminComponent;
  let fixture: ComponentFixture<ImportAdminComponent>;
  let importService: ImportAdminService;
  let savedFiltersService: SavedFiltersService;
  let store: ImportAdminStore;

  const savedFilter: SavedFilter = {
    id: 42, name: 'Reviewed proteins', filterJson: {}, createdAt: '2026-01-01T00:00:00Z',
  };

  async function loadFilters(filters: SavedFilter[] = [savedFilter]): Promise<void> {
    vi.spyOn(savedFiltersService, 'listSavedFilters').mockReturnValue(
      of({content: filters, page: 0, size: 200, totalElements: filters.length, totalPages: 1}),
    );
    component.onSourceModeChange(true);
    await fixture.whenStable();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ImportAdminComponent,

        MatPaginatorModule,
        MatTableModule,
        MatProgressBarModule,
        MatCardModule,
        MatFormFieldModule,
        MatSelectModule,
        FormsModule,
        MatButtonModule,
        MatIconModule,
      ],
      providers: [ImportAdminService, provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    importService = TestBed.inject(ImportAdminService);
    savedFiltersService = TestBed.inject(SavedFiltersService);
    vi.spyOn(importService, 'listImportJobs').mockReturnValue(
      of({content: [], page: 0, size: 5, totalElements: 0, totalPages: 0}),
    );
    vi.spyOn(savedFiltersService, 'listSavedFilters').mockReturnValue(
      of({content: [], page: 0, size: 200, totalElements: 0, totalPages: 0}),
    );
    fixture = TestBed.createComponent(ImportAdminComponent);
    component = fixture.componentInstance;
    store = fixture.debugElement.injector.get(ImportAdminStore);
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize signals with default values', () => {
    expect(component.isRemoteMode()).toBe(false);
    expect(component.selectedFile()).toBeNull();
    expect(component.selectedFilterId()).toBeNull();
    expect(component.savedFilters()).toEqual([]);
    expect(component.savedFiltersLoading()).toBe(false);
    expect(component.savedFiltersError()).toBeNull();
    expect(component.isUploading()).toBe(false);
    expect(component.currentProgress()).toBe(0);
    expect(component.errorMessage()).toBeNull();
    expect(component.jobHistory()).toEqual([]);
    expect(component.totalJobs()).toBe(0);
    expect(component.pageSize()).toBe(5);
    expect(component.pageIndex()).toBe(0);
  });

  it('cancels an in-flight submission on destruction', () => {
    const response = new Subject<ImportJobCreated>();
    vi.spyOn(importService, 'triggerImport').mockReturnValue(response);
    store.selectFile(new File(['content'], 'proteins.dat'));
    component.submitImport();
    expect(response.observed).toBe(true);

    fixture.destroy();

    expect(response.observed).toBe(false);
    expect(component.isUploading()).toBe(false);
  });

  describe('File Selection', () => {
    it('should accept valid .dat file', () => {
      const file = new File(['content'], 'test.dat', {type: 'text/plain'});
      const event = createFileSelectionEvent(file);

      component.onFileSelected(event);

      expect(component.selectedFile()).toBe(file);
      expect(component.errorMessage()).toBeNull();
    });

    it('should accept valid .tsv file', () => {
      const file = new File(['content'], 'test.tsv', {type: 'text/tab-separated-values'});
      const event = createFileSelectionEvent(file);

      component.onFileSelected(event);

      expect(component.selectedFile()).toBe(file);
      expect(component.errorMessage()).toBeNull();
    });

    it('should reject file with invalid extension', () => {
      const file = new File(['content'], 'test.pdf', {type: 'application/pdf'});
      const event = createFileSelectionEvent(file);

      component.onFileSelected(event);

      expect(component.selectedFile()).toBeNull();
      expect(component.errorMessage()).toContain('Unsupported file type');
    });

    it('should reject file larger than 2 GB', () => {
      const oversizedFile = new File(['content'], 'too-large.dat', {type: 'text/plain'});
      Object.defineProperty(oversizedFile, 'size', {
        value: 2 * 1024 * 1024 * 1024 + 1,
        configurable: true,
      });
      const event = createFileSelectionEvent(oversizedFile);

      component.onFileSelected(event);

      expect(component.selectedFile()).toBeNull();
      expect(component.errorMessage()).toContain('Payload Too Large');
    });
  });

  describe('Import Submission', () => {
    it('should not submit if no file selected', () => {
      const triggerSpy = vi.spyOn(importService, 'triggerImport');

      component.submitImport();

      expect(triggerSpy).not.toHaveBeenCalled();
    });

    it('should start polling when import creation succeeds', () => {
      const file = new File(['content'], 'proteins.dat', {type: 'text/plain'});
      vi.spyOn(importService, 'triggerImport').mockReturnValue(
        of({
          id: 'job-123',
          status: 'RUNNING',
          createdAt: new Date().toISOString(),
        }),
      );
      store.selectFile(file);

      component.submitImport();

      expect(component.isUploading()).toBe(true);
      expect(component.currentProgress()).toBe(0);
      expect(component.errorMessage()).toBeNull();
      expect(store.activeJobId()).toBe('job-123');
    });

    it('should set conflict message and stop uploading on 409 error', () => {
      const file = new File(['content'], 'proteins.dat', {type: 'text/plain'});
      vi.spyOn(importService, 'triggerImport').mockReturnValue(
        throwError(() => new HttpErrorResponse({status: 409})),
      );
      store.selectFile(file);

      component.submitImport();

      expect(component.isUploading()).toBe(false);
      expect(component.errorMessage()).toBe('Conflict: An import is already running.');
    });

    it.each([
      [413, 'Payload Too Large: The file size exceeds the 2 GB limit.'],
      [422, 'Unprocessable Entity: Unsupported file type.'],
      [500, 'An unexpected error occurred while communicating with server.'],
    ])('should map HTTP %s import errors to a user message', (status, message) => {
      const file = new File(['content'], 'proteins.dat', {type: 'text/plain'});
      vi.spyOn(importService, 'triggerImport').mockReturnValue(
        throwError(() => new HttpErrorResponse({status})),
      );
      store.selectFile(file);

      component.submitImport();

      expect(component.isUploading()).toBe(false);
      expect(component.errorMessage()).toBe(message);
    });

    it('should stop uploading when local import creation has no job id', () => {
      vi.spyOn(importService, 'triggerImport').mockReturnValue(
        of(undefined as unknown as ImportJobCreated),
      );
      store.selectFile(new File(['content'], 'proteins.dat'));

      component.submitImport();

      expect(component.isUploading()).toBe(false);
      expect(component.errorMessage()).toContain('job identifier');
    });

    it('should not submit a remote import without a selected filter', () => {
      const triggerSpy = vi.spyOn(importService, 'triggerRemoteImport');

      component.onSourceModeChange(true);
      component.submitImport();

      expect(triggerSpy).not.toHaveBeenCalled();
      expect(component.isUploading()).toBe(false);
    });

    it('should load saved filters when switching to remote mode', async () => {
      const filters: SavedFilter[] = [
        {id: 42, name: 'Reviewed proteins', filterJson: {}, createdAt: new Date().toISOString()},
      ];
      vi.spyOn(savedFiltersService, 'listSavedFilters').mockReturnValue(
        of({content: filters, page: 0, size: 200, totalElements: 1, totalPages: 1}),
      );

      component.onSourceModeChange(true);
      await fixture.whenStable();

      expect(component.isRemoteMode()).toBe(true);
      expect(component.savedFilters()).toEqual(filters);
      expect(component.savedFiltersLoading()).toBe(false);
      expect(component.savedFiltersError()).toBeNull();
    });

    it('should start polling when a remote filter is selected', async () => {
      await loadFilters();
      vi.spyOn(importService, 'triggerRemoteImport').mockReturnValue(
        of({
          id: 'remote-job-123',
          status: 'RUNNING',
          createdAt: new Date().toISOString(),
        }),
      );

      component.onFilterSelect(createFilterSelectionEvent('42'));

      expect(importService.triggerRemoteImport).toHaveBeenCalledWith(42);
      expect(component.isUploading()).toBe(true);
      expect(store.activeJobId()).toBe('remote-job-123');
    });

    it('should stop uploading when remote import creation has no job id', async () => {
      vi.spyOn(importService, 'triggerRemoteImport').mockReturnValue(
        of(undefined as unknown as ImportJobCreated),
      );
      await loadFilters();
      component.onFilterSelect(createFilterSelectionEvent('42'));

      expect(component.isUploading()).toBe(false);
      expect(component.errorMessage()).toContain('job identifier');
    });

    it('should not trigger a remote import for the placeholder option', () => {
      const triggerSpy = vi.spyOn(importService, 'triggerRemoteImport');

      component.onSourceModeChange(true);
      component.onFilterSelect(createFilterSelectionEvent(''));

      expect(component.selectedFilterId()).toBeNull();
      expect(triggerSpy).not.toHaveBeenCalled();
    });

    it('should ignore filter ids that are not in the loaded saved filters', async () => {
      const triggerSpy = vi.spyOn(importService, 'triggerRemoteImport');
      await loadFilters();

      component.onFilterSelect(createFilterSelectionEvent('99'));

      expect(component.selectedFilterId()).toBeNull();
      expect(triggerSpy).not.toHaveBeenCalled();
    });

    it('should expose an error when saved filters cannot be loaded', async () => {
      vi.spyOn(savedFiltersService, 'listSavedFilters').mockReturnValue(
        throwError(() => new HttpErrorResponse({status: 500})),
      );

      component.onSourceModeChange(true);
      await fixture.whenStable();

      expect(component.savedFiltersLoading()).toBe(false);
      expect(component.savedFiltersError()).toBe('Failed to load saved filters.');
    });
  });

  describe('Pagination', () => {
    it('should handle pagination change', () => {
      const pageEvent: PageEvent = {
        pageIndex: 1,
        pageSize: 10,
        length: 50,
      };

      component.onPageChange(pageEvent);

      expect(component.pageIndex()).toBe(1);
      expect(component.pageSize()).toBe(10);
    });
  });

  describe('Source mode and rendering states', () => {
    it('should clear local and remote selections when changing source', () => {
      store.selectFile(new File(['content'], 'proteins.dat'));

      component.onSourceModeChange(true);

      expect(component.selectedFile()).toBeNull();
      expect(component.selectedFilterId()).toBeNull();
      expect(component.isRemoteMode()).toBe(true);
    });

    it('should render remote loading state', () => {
      vi.spyOn(savedFiltersService, 'listSavedFilters').mockReturnValue(new Subject());
      component.onSourceModeChange(true);

      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain('Loading saved filters...');
    });

    it('should render remote error state', () => {
      vi.spyOn(savedFiltersService, 'listSavedFilters').mockReturnValue(throwError(() => new Error('Unavailable')));
      component.onSourceModeChange(true);

      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain('Failed to load saved filters.');
      expect(fixture.nativeElement.textContent).toContain('Retry');
    });

    it('should render an empty remote filter state', () => {
      component.onSourceModeChange(true);

      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain('No saved filters found.');
    });

    it('should render remote filter options and progress', async () => {
      vi.useFakeTimers({toFake: ['setInterval', 'clearInterval']});
      try {
        await loadFilters();
        vi.spyOn(importService, 'triggerRemoteImport').mockReturnValue(of({
          id: 'remote-job', status: 'RUNNING', createdAt: '2026-01-01T00:00:00Z',
        }));
        vi.spyOn(importService, 'getJobProgress').mockReturnValue(of<ImportJobProgress>({
          id: 'remote-job', status: 'RUNNING', fileName: null, recordsProcessed: 37,
          totalEstimated: 100, progressPercent: 37, elapsedMs: 3000, errorMessage: null,
        }));
        component.onFilterSelect(createFilterSelectionEvent('42'));
        await vi.advanceTimersByTimeAsync(3000);

        fixture.detectChanges();

        expect(fixture.nativeElement.textContent).toContain('Reviewed proteins');
        expect(fixture.nativeElement.textContent).toContain('37%');
      } finally {
        fixture.destroy();
        vi.useRealTimers();
      }
    });
  });

  describe('Job History', () => {
    beforeEach(() => {
      vi.useFakeTimers({toFake: ['setInterval', 'clearInterval']});
    });

    afterEach(() => {
      fixture.destroy();
      vi.useRealTimers();
    });

    it('shows an accessible indeterminate bar until history finishes loading', async () => {
      const response = new Subject<PagedResponse<ImportJobSummary>>();
      vi.spyOn(importService, 'listImportJobs').mockReturnValue(response);
      fixture.detectChanges();
      await fixture.whenStable();

      const root = fixture.nativeElement as HTMLElement;
      const progressBar = root.querySelector('mat-progress-bar');
      expect(progressBar?.getAttribute('aria-label')).toBe('Loading import history…');
      expect(progressBar?.getAttribute('aria-valuenow')).toBeNull();
      expect(root.querySelector('.history-table-wrapper')?.getAttribute('aria-busy')).toBe('true');
      const emptyRow = root.querySelector<HTMLElement>('.history-empty-row');
      if (!emptyRow) throw new Error('Expected the empty-history row');
      expect(getComputedStyle(emptyRow).display).toBe('none');

      await vi.advanceTimersByTimeAsync(3000);
      expect(importService.listImportJobs).toHaveBeenCalledWith(0, 5);
      response.next({content: [], totalElements: 0, totalPages: 0, page: 0, size: 5});
      response.complete();
      await fixture.whenStable();

      expect(root.querySelector('mat-progress-bar')).toBeNull();
      expect(root.querySelector('.history-table-wrapper')?.getAttribute('aria-busy')).toBe('false');
      expect(root.textContent).toContain('No import history found.');
      expect(getComputedStyle(emptyRow).display).not.toBe('none');
    });

    it('clears history loading when a request fails', async () => {
      vi.spyOn(importService, 'listImportJobs').mockReturnValue(
        throwError(() => new HttpErrorResponse({status: 500})),
      );
      fixture.detectChanges();
      await fixture.whenStable();
      await vi.advanceTimersByTimeAsync(3000);
      await fixture.whenStable();

      expect(component.historyLoading()).toBe(false);
      expect((fixture.nativeElement as HTMLElement).querySelector('mat-progress-bar')).toBeNull();
      expect(component.errorMessage()).toBe('Failed to load import job history.');
    });

    it('shows loading again when the history page changes', async () => {
      vi.spyOn(importService, 'listImportJobs').mockReturnValue(
        of({content: [], totalElements: 0, totalPages: 0, page: 0, size: 5}),
      );
      fixture.detectChanges();
      await fixture.whenStable();
      await vi.advanceTimersByTimeAsync(3000);
      await fixture.whenStable();
      expect(component.historyLoading()).toBe(false);

      component.onPageChange({pageIndex: 1, pageSize: 10, length: 20});
      await fixture.whenStable();

      expect(component.historyLoading()).toBe(true);
      expect((fixture.nativeElement as HTMLElement).querySelector('mat-progress-bar')).not.toBeNull();
      await vi.advanceTimersByTimeAsync(3000);
      expect(importService.listImportJobs).toHaveBeenLastCalledWith(1, 10);
      expect(component.historyLoading()).toBe(false);
    });

    it('cancels an in-flight history request and clears loading on destruction', async () => {
      const response = new Subject<PagedResponse<ImportJobSummary>>();
      vi.spyOn(importService, 'listImportJobs').mockReturnValue(response);
      fixture.detectChanges();
      await fixture.whenStable();
      await vi.advanceTimersByTimeAsync(3000);
      expect(response.observed).toBe(true);

      fixture.destroy();

      expect(response.observed).toBe(false);
      expect(component.historyLoading()).toBe(false);
    });

    it('should display job history in table', async () => {
      const mockJobs: ImportJobSummary[] = [
        {
          id: 'job-1',
          status: 'COMPLETED',
          fileName: 'proteins.dat',
          progressPercent: 100,
          entryCount: 1000,
          durationMs: 5000,
          createdAt: new Date().toISOString(),
          completedAt: new Date().toISOString(),
          errorMessage: null,
        },
      ];

      vi.spyOn(importService, 'listImportJobs').mockReturnValue(of({
        content: mockJobs, totalElements: 1, totalPages: 1, page: 0, size: 5,
      }));
      fixture.detectChanges();
      await fixture.whenStable();
      await vi.advanceTimersByTimeAsync(3000);
      await fixture.whenStable();

      expect(component.jobHistory().length).toBe(1);
      expect(component.totalJobs()).toBe(1);
      expect((fixture.nativeElement as HTMLElement).querySelector('.history-table')?.textContent)
        .toContain('proteins.dat');
    });
  });

  describe('File Input Trigger', () => {
    it('should trigger file input click', () => {
      fixture.detectChanges();
      const input = (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>('input[type="file"]');
      if (!input) throw new Error('Expected the file input');
      const clickSpy = vi.spyOn(input, 'click');

      component.triggerFileInput();

      expect(clickSpy).toHaveBeenCalled();
    });
  });
});


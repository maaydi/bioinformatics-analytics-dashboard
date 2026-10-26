import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ImportJobCreated, ImportJobProgress, ImportJobSummary} from '@core/models/import.model';
import {PagedResponse} from '@core/models/paged-response.model';
import {SavedFilter} from '@core/models/saved-filter.model';
import {SavedFiltersService} from '@features/saved-filters/saved-filters.service';
import {of, Subject, throwError} from 'rxjs';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {IMPORT_ADMIN_MESSAGES as messages} from './import-admin.messages';
import {ImportAdminService} from './import-admin.service';
import {ImportAdminStore} from './import-admin.store';

@Component({
  template: '',
  providers: [ImportAdminStore],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
class StoreHost {
  readonly store = inject(ImportAdminStore);
}

const created: ImportJobCreated = {id: 'job-1', status: 'RUNNING', createdAt: '2026-01-01T00:00:00Z'};
const savedFilter: SavedFilter = {id: 42, name: 'Reviewed', filterJson: {}, createdAt: created.createdAt};
const completedJob: ImportJobSummary = {
  ...created, status: 'COMPLETED', fileName: 'proteins.dat', progressPercent: 100,
  entryCount: 10, durationMs: 1000, completedAt: created.createdAt, errorMessage: null,
};

function progress(status: ImportJobProgress['status'], percent = 37): ImportJobProgress {
  return {
    ...created, status, fileName: null, recordsProcessed: percent, totalEstimated: 100,
    progressPercent: percent, elapsedMs: 3000, errorMessage: null,
  };
}

describe('ImportAdminStore', () => {
  let fixture: ComponentFixture<StoreHost>;
  let store: ImportAdminStore;
  const listImportJobs = vi.fn<ImportAdminService['listImportJobs']>();
  const triggerImport = vi.fn<ImportAdminService['triggerImport']>();
  const triggerRemoteImport = vi.fn<ImportAdminService['triggerRemoteImport']>();
  const getJobProgress = vi.fn<ImportAdminService['getJobProgress']>();
  const listSavedFilters = vi.fn<SavedFiltersService['listSavedFilters']>();

  async function poll(): Promise<void> {
    await vi.advanceTimersByTimeAsync(3000);
    await fixture.whenStable();
  }

  beforeEach(async () => {
    vi.useFakeTimers({toFake: ['setInterval', 'clearInterval']});
    listImportJobs.mockReset().mockReturnValue(of({
      content: [completedJob], page: 0, size: 5, totalElements: 1, totalPages: 1,
    }));
    triggerImport.mockReset().mockReturnValue(of(created));
    triggerRemoteImport.mockReset().mockReturnValue(of(created));
    getJobProgress.mockReset().mockReturnValue(of(progress('RUNNING')));
    listSavedFilters.mockReset().mockReturnValue(of({
      content: [savedFilter], page: 0, size: 200, totalElements: 1, totalPages: 1,
    }));
    await TestBed.configureTestingModule({
      imports: [StoreHost],
      providers: [
        {provide: ImportAdminService, useValue: {listImportJobs, triggerImport, triggerRemoteImport, getJobProgress}},
        {provide: SavedFiltersService, useValue: {listSavedFilters}},
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(StoreHost);
    store = fixture.componentInstance.store;
    await fixture.whenStable();
  });

  afterEach(() => {
    fixture.destroy();
    vi.useRealTimers();
  });

  it('waits until the browser renders before loading history', async () => {
    expect(store.historyLoading()).toBe(true);
    expect(listImportJobs).not.toHaveBeenCalled();
    await poll();
    expect(listImportJobs).toHaveBeenCalledExactlyOnceWith(0, 5);
    expect(store.jobHistory()).toEqual([completedJob]);
    expect(store.totalJobs()).toBe(1);
    expect(store.historyLoading()).toBe(false);
    await poll();
    expect(listImportJobs).toHaveBeenCalledOnce();
  });

  it('cancels stale history requests when pagination changes', async () => {
    const first = new Subject<PagedResponse<ImportJobSummary>>();
    const second = new Subject<PagedResponse<ImportJobSummary>>();
    listImportJobs.mockReturnValueOnce(first).mockReturnValueOnce(second);
    await poll();
    expect(first.observed).toBe(true);
    store.changePage(1, 10);
    await fixture.whenStable();
    expect(first.observed).toBe(false);
    expect(store.historyLoading()).toBe(true);
    await poll();
    expect(listImportJobs).toHaveBeenLastCalledWith(1, 10);
    second.next({content: [], page: 1, size: 10, totalElements: 10, totalPages: 1});
    second.complete();
    expect(store.jobHistory()).toEqual([]);
    expect(store.totalJobs()).toBe(10);
    expect(store.historyLoading()).toBe(false);
  });

  it('does not cancel slow history requests on polling ticks', async () => {
    const response = new Subject<PagedResponse<ImportJobSummary>>();
    listImportJobs.mockReturnValue(response);
    await poll();
    await poll();
    expect(listImportJobs).toHaveBeenCalledOnce();
    expect(response.observed).toBe(true);
    expect(store.historyLoading()).toBe(true);
  });

  it('recovers history polling after an HTTP failure', async () => {
    listImportJobs.mockReturnValueOnce(throwError(() => new Error('Unavailable')));
    await poll();
    expect(store.errorMessage()).toBe(messages.historyLoadFailed);
    expect(store.historyLoading()).toBe(false);
    await poll();
    expect(store.jobHistory()).toEqual([completedJob]);
    expect(listImportJobs).toHaveBeenCalledTimes(2);
  });

  it('loads every saved-filter page before clearing loading', async () => {
    const secondPage = new Subject<PagedResponse<SavedFilter>>();
    const secondFilter = {...savedFilter, id: 43};
    listSavedFilters.mockReturnValueOnce(of({
      content: [savedFilter], page: 0, size: 200, totalElements: 2, totalPages: 2,
    })).mockReturnValueOnce(secondPage);
    store.selectSource(true);
    await fixture.whenStable();
    expect(listSavedFilters.mock.calls).toEqual([[0, 200], [1, 200]]);
    expect(store.savedFiltersLoading()).toBe(true);
    secondPage.next({content: [secondFilter], page: 1, size: 200, totalElements: 2, totalPages: 2});
    secondPage.complete();
    expect(store.savedFilters()).toEqual([savedFilter, secondFilter]);
    expect(store.savedFiltersLoading()).toBe(false);
  });

  it('cancels filter loading on source changes and ignores late responses', async () => {
    const response = new Subject<PagedResponse<SavedFilter>>();
    listSavedFilters.mockReturnValue(response);
    store.selectSource(true);
    await fixture.whenStable();
    expect(response.observed).toBe(true);
    store.selectSource(false);
    await fixture.whenStable();
    expect(response.observed).toBe(false);
    expect(store.savedFiltersLoading()).toBe(false);
    response.next({content: [savedFilter], page: 0, size: 200, totalElements: 1, totalPages: 1});
    expect(store.savedFilters()).toEqual([]);
  });

  it('can explicitly retry a failed filter load', async () => {
    listSavedFilters.mockReturnValueOnce(throwError(() => new Error('Unavailable')));
    store.selectSource(true);
    await fixture.whenStable();
    expect(store.savedFiltersError()).toBe(messages.filtersLoadFailed);
    store.selectSource(true);
    await fixture.whenStable();
    expect(store.savedFiltersError()).toBeNull();
    expect(store.savedFilters()).toEqual([savedFilter]);
  });

  it('prevents duplicate submissions and source changes until the job finishes', async () => {
    const response = new Subject<ImportJobCreated>();
    triggerImport.mockReturnValue(response);
    const file = new File(['content'], 'proteins.dat');
    store.selectFile(file);
    store.submitImport();
    store.submitImport();
    store.selectSource(true);
    store.selectFile(new File(['other'], 'other.tsv'));
    expect(triggerImport).toHaveBeenCalledExactlyOnceWith(file);
    expect(store.isRemoteMode()).toBe(false);
    expect(store.selectedFile()).toBe(file);
    response.next(created);
    await poll();
    expect(store.activeJobId()).toBe(created.id);
    expect(store.currentProgress()).toBe(37);
    expect(getJobProgress).toHaveBeenCalledExactlyOnceWith(created.id);
  });

  it.each(['COMPLETED', 'FAILED'] as const)('stops polling on %s and refreshes final history', async status => {
    store.selectFile(new File(['content'], 'proteins.dat'));
    getJobProgress.mockReturnValue(of(progress(status, 100)));
    store.submitImport();
    await poll();
    expect(store.currentProgress()).toBe(100);
    expect(store.isUploading()).toBe(false);
    expect(store.activeJobId()).toBeNull();
    expect(store.selectedFile()).toBeNull();
    expect(store.errorMessage()).toBe(status === 'FAILED' ? messages.jobFailed(created.id) : null);
    await poll();
    expect(getJobProgress).toHaveBeenCalledOnce();
    expect(listImportJobs).toHaveBeenCalledTimes(2);
  });

  it('handles progress errors without automatically repeating an accepted submission', async () => {
    getJobProgress.mockReturnValue(throwError(() => new Error('Disconnected')));
    store.selectFile(new File(['content'], 'proteins.dat'));
    store.submitImport();
    await poll();
    expect(store.errorMessage()).toBe(messages.statusConnectionLost);
    expect(store.isUploading()).toBe(false);
    expect(store.activeJobId()).toBeNull();
    await poll();
    expect(triggerImport).toHaveBeenCalledOnce();
    expect(getJobProgress).toHaveBeenCalledOnce();
  });

  it('validates remote filter identifiers against loaded options', async () => {
    store.selectSource(true);
    await fixture.whenStable();
    for (const invalidId of ['', ' ', 'Infinity', '42.5', '43']) {
      store.selectFilter(invalidId);
      expect(store.selectedFilterId()).toBeNull();
    }
    expect(triggerRemoteImport).not.toHaveBeenCalled();
    store.selectFilter('42');
    expect(triggerRemoteImport).toHaveBeenCalledExactlyOnceWith(42);
    expect(store.isUploading()).toBe(true);
  });

  it('tears down pending filter and progress requests on destruction', async () => {
    const filters = new Subject<PagedResponse<SavedFilter>>();
    const status = new Subject<ImportJobProgress>();
    listSavedFilters.mockReturnValue(filters);
    store.selectFile(new File(['content'], 'proteins.dat'));
    store.submitImport();
    getJobProgress.mockReturnValue(status);
    await poll();
    expect(status.observed).toBe(true);
    fixture.destroy();
    expect(status.observed).toBe(false);
    expect(store.isUploading()).toBe(false);
    expect(store.activeJobId()).toBeNull();

    fixture = TestBed.createComponent(StoreHost);
    store = fixture.componentInstance.store;
    store.selectSource(true);
    await fixture.whenStable();
    expect(filters.observed).toBe(true);
    fixture.destroy();
    expect(filters.observed).toBe(false);
    expect(store.savedFiltersLoading()).toBe(false);
  });

  it('starts with fresh state when the feature is reopened', () => {
    store.selectFile(new File(['content'], 'proteins.dat'));
    store.changePage(2, 20);
    fixture.destroy();
    fixture = TestBed.createComponent(StoreHost);
    const freshStore = fixture.componentInstance.store;
    expect(freshStore).not.toBe(store);
    expect(freshStore.selectedFile()).toBeNull();
    expect(freshStore.pageIndex()).toBe(0);
    expect(freshStore.pageSize()).toBe(5);
    expect(freshStore.jobHistory()).toEqual([]);
  });
});

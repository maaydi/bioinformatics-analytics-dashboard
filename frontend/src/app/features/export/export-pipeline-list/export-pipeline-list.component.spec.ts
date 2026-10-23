import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatDialog} from '@angular/material/dialog';
import {provideRouter} from '@angular/router';
import {ExportJobStatus, ExportPipeline, ExportStatus} from '@core/models/export-pipeline.model';
import {PagedResponse} from '@core/models/paged-response.model';
import {NotificationService} from '@shared/directive/notification.service';
import {of, Subject, throwError} from 'rxjs';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {ExportPipelineService} from '../export-pipeline.service';
import {ExportPipelineListComponent} from './export-pipeline-list.component';
import {ExportPipelineListStore, ExportPipelineRow} from './export-pipeline-list.store';

function pipeline(id = 1, status: ExportStatus = 'COMPLETED'): ExportPipeline {
  return {id, name: `Export ${id}`, format: 'CSV', fieldSchema: ['accession'], status,
    actualRows: 0, fileSizeBytes: 0, createdAt: '2026-01-01T00:00:00Z'};
}

function page(content: ExportPipeline[], totalElements = content.length): PagedResponse<ExportPipeline> {
  return {content, totalElements, totalPages: Math.ceil(totalElements / 5), page: 0, size: 5};
}

describe('ExportPipelineListComponent', () => {
  let fixture: ComponentFixture<ExportPipelineListComponent>;
  let store: ExportPipelineListStore;
  let root: HTMLElement;
  let confirmation: Subject<boolean | undefined>;
  const api = {
    listPipelines: vi.fn<ExportPipelineService['listPipelines']>(),
    getStatus: vi.fn<ExportPipelineService['getStatus']>(),
    retryPipeline: vi.fn<ExportPipelineService['retryPipeline']>(),
    deletePipeline: vi.fn<ExportPipelineService['deletePipeline']>(),
    getDownloadUrl: vi.fn<ExportPipelineService['getDownloadUrl']>(),
    downloadFile: vi.fn<ExportPipelineService['downloadFile']>(),
  };
  const notify = {success: vi.fn(), error: vi.fn()};
  const open = vi.fn();

  function row(): ExportPipelineRow {
    const first = store.rows()[0];
    if (!first) throw new Error('Expected a pipeline row');
    return first;
  }

  function button(label: string): HTMLButtonElement {
    const result = Array.from(root.querySelectorAll<HTMLButtonElement>('button'))
      .find(element => element.getAttribute('aria-label') === label);
    if (!result) throw new Error(`Missing action: ${label}`);
    return result;
  }

  async function render(content: ExportPipeline[], total = content.length): Promise<void> {
    api.listPipelines.mockReturnValue(of(page(content, total)));
    fixture.detectChanges();
    await fixture.whenStable();
  }

  beforeEach(async () => {
    vi.resetAllMocks();
    // Fake only RxJS's interval clock; Angular rendering still uses real async scheduling.
    vi.useFakeTimers({toFake: ['setInterval', 'clearInterval']});
    confirmation = new Subject<boolean | undefined>();
    open.mockReturnValue({afterClosed: () => confirmation.asObservable()});
    api.listPipelines.mockReturnValue(of(page([])));
    api.deletePipeline.mockReturnValue(of(undefined));
    api.retryPipeline.mockReturnValue(of(pipeline(2, 'QUEUED')));
    await TestBed.configureTestingModule({
      imports: [ExportPipelineListComponent],
      providers: [provideRouter([]), {provide: ExportPipelineService, useValue: api},
        {provide: NotificationService, useValue: notify}, {provide: MatDialog, useValue: {open}}],
    }).compileComponents();
    fixture = TestBed.createComponent(ExportPipelineListComponent);
    store = fixture.debugElement.injector.get(ExportPipelineListStore);
    root = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => {
    fixture.destroy();
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('loads immediately with import-style pagination and preserves zero values', async () => {
    await render([pipeline()]);
    expect(api.listPipelines).toHaveBeenCalledExactlyOnceWith(undefined, 0, 5);
    expect(root.textContent).toContain('Export 1');
    expect(row().rows).toBe(0);
    expect(row().size).toBe('0 B');
    expect(root.querySelector('a')?.getAttribute('href')).toBe('/genes');
  });

  it('renders the correct badge and icon for every state', async () => {
    const statuses: ExportStatus[] = ['QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'];
    await render(statuses.map((status, index) => pipeline(index + 1, status)));
    expect(Array.from(root.querySelectorAll('.status-badge')).map(badge => badge.textContent?.trim()))
      .toEqual(['hourglass_empty Queued', 'sync Running', 'check_circle Completed', 'error Failed', 'cancel Cancelled']);
    expect(root.querySelectorAll('mat-progress-bar')).toHaveLength(1);
  });

  it('paginates server-side, resets page on filtering, and cancels stale requests', async () => {
    await render([pipeline()], 21);
    const stale = new Subject<PagedResponse<ExportPipeline>>();
    api.listPipelines.mockReturnValueOnce(stale);
    store.changePage(2, 5);
    await fixture.whenStable();
    expect(stale.observed).toBe(true);
    store.selectStatus('FAILED');
    await fixture.whenStable();
    expect(stale.observed).toBe(false);
    expect(api.listPipelines).toHaveBeenLastCalledWith('FAILED', 0, 5);
    expect(store.page()).toBe(0);
  });

  it('keeps the selected filter selected when clicked again', async () => {
    await render([]);
    const chips = Array.from(root.querySelectorAll<HTMLButtonElement>('[role="option"]'));
    chips.find(chip => chip.textContent?.trim() === 'Failed')?.click();
    await fixture.whenStable();
    expect(store.status()).toBe('FAILED');
    const selected = root.querySelector<HTMLButtonElement>('[role="option"][aria-selected="true"]');
    selected?.click();
    await fixture.whenStable();
    expect(root.querySelector('[role="option"][aria-selected="true"]')?.textContent).toContain('Failed');
  });

  it('supports keyboard selection of status filters', async () => {
    await render([]);
    const option = Array.from(root.querySelectorAll<HTMLButtonElement>('[role="option"]'))
      .find(element => element.textContent?.trim() === 'Running');
    if (!option) throw new Error('Expected the running filter');
    option.dispatchEvent(new KeyboardEvent('keydown', {key: ' ', keyCode: 32, bubbles: true}));
    await fixture.whenStable();
    expect(store.status()).toBe('RUNNING');
    expect(option.getAttribute('aria-selected')).toBe('true');
  });

  it('shows a useful empty state and a recoverable load error', async () => {
    await render([]);
    expect(root.textContent).toContain('No export pipelines yet.');
    api.listPipelines.mockReturnValueOnce(throwError(() => new Error('offline')));
    store.refresh();
    await fixture.whenStable();
    expect(root.querySelector('[role="alert"]')?.textContent).toContain('could not be loaded');
    expect(store.loading()).toBe(false);
    store.refresh();
    await fixture.whenStable();
    expect(store.error()).toBeNull();
  });

  it('offers download only when completed and retry only when failed', async () => {
    await render([pipeline(1), pipeline(2, 'FAILED'), pipeline(3, 'RUNNING')]);
    expect(button('Download Export 1')).toBeTruthy();
    expect(button('Retry Export 2')).toBeTruthy();
    expect(root.querySelector('[aria-label="Download Export 3"]')).toBeNull();
    expect(root.querySelector('[aria-label="Retry Export 1"]')).toBeNull();
  });

  it('prevents duplicate retries and reports retry failures', async () => {
    await render([pipeline(1, 'FAILED')]);
    const response = new Subject<ExportPipeline>();
    api.retryPipeline.mockReturnValue(response);
    const failed = row();
    store.retry(failed);
    store.retry(failed);
    expect(api.retryPipeline).toHaveBeenCalledExactlyOnceWith(1);
    expect(row().busy).toBe(true);
    response.error(new Error('conflict'));
    expect(notify.error).toHaveBeenCalledWith('The export could not be retried. Please try again.');
    expect(row().busy).toBe(false);
  });

  it('refreshes after a successful retry', async () => {
    await render([pipeline(1, 'FAILED')]);
    button('Retry Export 1').click();
    await fixture.whenStable();
    expect(api.retryPipeline).toHaveBeenCalledWith(1);
    expect(api.listPipelines).toHaveBeenCalledTimes(2);
    expect(notify.success).toHaveBeenCalled();
  });

  it.each([false, undefined])('does not delete on confirmation cancellation %s', async result => {
    await render([pipeline()]);
    button('Delete Export 1').click();
    confirmation.next(result);
    confirmation.complete();
    expect(api.deletePipeline).not.toHaveBeenCalled();
  });

  it('deletes after confirmation and corrects a now-empty last page', async () => {
    await render([pipeline()], 6);
    store.changePage(1, 5);
    await fixture.whenStable();
    button('Delete Export 1').click();
    expect(open).toHaveBeenCalled();
    api.listPipelines.mockReturnValueOnce(of(page([], 5))).mockReturnValue(of(page([pipeline(2)], 5)));
    confirmation.next(true);
    confirmation.complete();
    await fixture.whenStable();
    expect(api.deletePipeline).toHaveBeenCalledExactlyOnceWith(1);
    expect(store.page()).toBe(0);
    expect(api.listPipelines).toHaveBeenLastCalledWith(undefined, 0, 5);
  });

  it('reports delete errors and preserves the row', async () => {
    await render([pipeline()]);
    api.deletePipeline.mockReturnValueOnce(throwError(() => new Error('offline')));
    store.delete(row());
    expect(notify.error).toHaveBeenCalledWith('The export could not be deleted. Please try again.');
    expect(row().busy).toBe(false);
  });

  it('polls queued/running jobs, updates progress, fetches terminal metadata and stops', async () => {
    await render([pipeline(1, 'QUEUED')]);
    api.getStatus.mockReturnValueOnce(of({pipelineId: 1, status: 'RUNNING', progressPercent: 42}));
    await vi.advanceTimersByTimeAsync(3000);
    await fixture.whenStable();
    expect(row().status).toBe('RUNNING');
    expect(row().progress).toBe(42);
    api.getStatus.mockReturnValueOnce(of({pipelineId: 1, status: 'COMPLETED', progressPercent: 100}));
    api.listPipelines.mockReturnValue(of(page([{...pipeline(), actualRows: 15, fileSizeBytes: 15000}])));
    await vi.advanceTimersByTimeAsync(3000);
    await fixture.whenStable();
    expect(row().size).toBe('15 KB');
    expect(row().rows).toBe(15);
    await vi.advanceTimersByTimeAsync(9000);
    expect(api.getStatus).toHaveBeenCalledTimes(2);
  });

  it('refreshes a queued-only filter when a job starts running', async () => {
    await render([pipeline(1, 'QUEUED')]);
    store.selectStatus('QUEUED');
    await fixture.whenStable();
    api.getStatus.mockReturnValueOnce(of({pipelineId: 1, status: 'RUNNING', progressPercent: 10}));
    api.listPipelines.mockReturnValue(of(page([])));
    await vi.advanceTimersByTimeAsync(3000);
    await fixture.whenStable();
    expect(store.rows()).toHaveLength(0);
    expect(api.listPipelines).toHaveBeenLastCalledWith('QUEUED', 0, 5);
  });

  it('retries failed polls without repeated toasts and cleans up requests on destruction', async () => {
    await render([pipeline(1, 'RUNNING')]);
    api.getStatus.mockReturnValue(throwError(() => new Error('offline')));
    await vi.advanceTimersByTimeAsync(6000);
    expect(notify.error).toHaveBeenCalledTimes(1);
    const status = new Subject<ExportJobStatus>();
    api.getStatus.mockReturnValue(status);
    await vi.advanceTimersByTimeAsync(3000);
    expect(status.observed).toBe(true);
    fixture.destroy();
    expect(status.observed).toBe(false);
    await vi.advanceTimersByTimeAsync(6000);
    expect(api.getStatus).toHaveBeenCalledTimes(3);
  });

  it('downloads authenticated blobs with metadata filenames and cleans up the anchor', async () => {
    await render([pipeline()]);
    const blob = new Blob(['accession\nP12345'], {type: 'text/csv'});
    api.getDownloadUrl.mockReturnValue(of({downloadUrl: '/unused', filename: 'export.csv', fileSizeBytes: 16, contentType: 'text/csv'}));
    api.downloadFile.mockReturnValue(of(blob));
    const create = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:export');
    const revoke = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) {
      expect(this.download).toBe('export.csv');
      expect(this.isConnected).toBe(true);
    });
    button('Download Export 1').click();
    expect(api.downloadFile).toHaveBeenCalledWith(1);
    expect(create).toHaveBeenCalledWith(blob);
    expect(click).toHaveBeenCalledOnce();
    expect(root.ownerDocument.querySelector('a[download]')).toBeNull();
    await new Promise<void>(resolve => setTimeout(resolve, 0));
    expect(revoke).toHaveBeenCalledWith('blob:export');
  });

  it('reports failed downloads and releases the row busy state', async () => {
    await render([pipeline()]);
    api.getDownloadUrl.mockReturnValue(of({downloadUrl: '/unused', filename: 'export.csv', fileSizeBytes: 0, contentType: 'text/csv'}));
    api.downloadFile.mockReturnValue(throwError(() => new Error('expired')));
    store.download(row());
    expect(notify.error).toHaveBeenCalledWith('The export could not be downloaded. Please try again.');
    expect(row().busy).toBe(false);
  });
});

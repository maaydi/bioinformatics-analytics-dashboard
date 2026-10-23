import {DOCUMENT} from '@angular/common';
import {
  afterNextRender,
  computed,
  DestroyRef,
  effect,
  inject,
  Injectable,
  LOCALE_ID,
  signal,
  untracked
} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ExportJobStatus, ExportPipeline, ExportStatus} from '@core/models/export-pipeline.model';
import {NotificationService} from '@shared/directive/notification.service';
import {catchError, EMPTY, exhaustMap, finalize, forkJoin, takeWhile, tap, timer} from 'rxjs';
import {ExportPipelineService} from '../export-pipeline.service';
import {formatExportBytes} from '../export-size-estimator';
import {EXPORT_PIPELINE_LIST_MESSAGES as messages, EXPORT_STATUS_PRESENTATION} from './export-pipeline-list.messages';

const STATUS_POLL_INTERVAL_MS = 3000;

interface ListQuery {
  readonly status: ExportStatus | undefined;
  readonly page: number;
  readonly size: number;
  readonly revision: number;
}

export interface ExportPipelineRow {
  readonly id: number;
  readonly name: string;
  readonly description: string | undefined;
  readonly format: string;
  readonly rows: number | undefined;
  readonly size: string;
  readonly status: ExportStatus;
  readonly statusLabel: string;
  readonly statusIcon: string;
  readonly statusClass: string;
  readonly createdAt: string;
  readonly errorMessage: string | undefined;
  readonly progress: number | null;
  readonly busy: boolean;
}

function isActive(status: ExportStatus): boolean {
  return status === 'QUEUED' || status === 'RUNNING';
}

@Injectable()
export class ExportPipelineListStore {
  private readonly api = inject(ExportPipelineService);
  private readonly notify = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);
  private readonly locale = inject(LOCALE_ID);
  private readonly ready = signal(false);
  private readonly pipelines = signal<readonly ExportPipeline[]>([]);
  private readonly progress = signal<Readonly<Record<number, ExportJobStatus>>>({});
  private readonly pendingIds = signal<readonly number[]>([]);
  private readonly query = signal<ListQuery>({status: undefined, page: 0, size: 5, revision: 0});
  private readonly loadingState = signal(true);
  private readonly errorState = signal<string | null>(null);
  private readonly totalState = signal(0);

  readonly loading = this.loadingState.asReadonly();
  readonly error = this.errorState.asReadonly();
  readonly total = this.totalState.asReadonly();
  readonly status = computed(() => this.query().status);
  readonly page = computed(() => this.query().page);
  readonly size = computed(() => this.query().size);
  readonly rows = computed<readonly ExportPipelineRow[]>(() => this.pipelines().map(pipeline => {
    const presentation = EXPORT_STATUS_PRESENTATION[pipeline.status];
    const percent = this.progress()[pipeline.id]?.progressPercent;
    return {
      id: pipeline.id, name: pipeline.name, description: pipeline.description, format: pipeline.format,
      rows: pipeline.actualRows ?? pipeline.estimatedRows,
      size: pipeline.fileSizeBytes == null ? messages.missing : formatExportBytes(pipeline.fileSizeBytes, this.locale),
      status: pipeline.status, statusLabel: presentation.label, statusIcon: presentation.icon,
      statusClass: presentation.className, createdAt: pipeline.createdAt, errorMessage: pipeline.errorMessage,
      progress: percent == null || !Number.isFinite(percent) ? null : Math.max(0, Math.min(100, percent)),
      busy: this.pendingIds().includes(pipeline.id),
    };
  }));
  private readonly activeIds = computed(
    () => this.pipelines().filter(pipeline => isActive(pipeline.status)).map(pipeline => pipeline.id),
    {equal: (previous, current) => previous.length === current.length && previous.every((id, index) => id === current[index])},
  );

  constructor() {
    afterNextRender(() => this.ready.set(true));
    effect(onCleanup => {
      if (!this.ready()) return;
      const query = this.query();
      const subscription = untracked(() => {
        this.loadingState.set(true);
        this.errorState.set(null);
        this.pipelines.set([]);
        this.progress.set({});
        return this.api.listPipelines(query.status, query.page, query.size).pipe(
          catchError(() => {
            this.errorState.set(messages.loadFailed);
            this.loadingState.set(false);
            return EMPTY;
          }),
        ).subscribe(response => {
          const lastPage = Math.max(0, Math.ceil(response.totalElements / query.size) - 1);
          if (query.page > lastPage) {
            this.query.update(current => ({...current, page: lastPage}));
            return;
          }
          this.pipelines.set(response.content);
          this.totalState.set(response.totalElements);
          this.loadingState.set(false);
        });
      });
      onCleanup(() => subscription.unsubscribe());
    });
    effect(onCleanup => {
      const ids = this.activeIds();
      // HTTP polling is asynchronous orchestration; local UI state remains in signals.
      const subscriptions = ids.map(id => {
        let errorReported = false;
        return timer(STATUS_POLL_INTERVAL_MS, STATUS_POLL_INTERVAL_MS).pipe(
          exhaustMap(() => this.api.getStatus(id).pipe(catchError(() => {
            if (!errorReported) this.notify.error(messages.pollFailed);
            errorReported = true;
            return EMPTY;
          }))),
          takeWhile(status => isActive(status.status), true),
        ).subscribe(status => {
          errorReported = false;
          this.progress.update(progress => ({...progress, [id]: status}));
          this.pipelines.update(pipelines => pipelines.map(pipeline => pipeline.id === id
            ? {...pipeline, status: status.status} : pipeline));
          // Fetch final row counts, file sizes and failure details not returned by /status.
          const filter = untracked(() => this.status());
          if (!isActive(status.status) || (filter !== undefined && filter !== status.status)) this.refresh();
        });
      });
      onCleanup(() => subscriptions.forEach(subscription => subscription.unsubscribe()));
    });
  }

  selectStatus(status: ExportStatus | undefined): void {
    if (status === this.status()) return;
    this.query.update(query => ({...query, status, page: 0}));
  }

  changePage(page: number, size: number): void {
    this.query.update(query => ({...query, page, size}));
  }

  refresh(): void {
    this.query.update(query => ({...query, revision: query.revision + 1}));
  }

  retry(row: ExportPipelineRow): void {
    if (row.status !== 'FAILED' || !this.beginAction(row.id)) return;
    this.api.retryPipeline(row.id).pipe(
      catchError(() => {
        this.notify.error(messages.retryFailed);
        return EMPTY;
      }),
      finalize(() => this.finishAction(row.id)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => {
      this.notify.success(messages.retried(row.name));
      this.refresh();
    });
  }

  delete(row: ExportPipelineRow): void {
    if (!this.beginAction(row.id)) return;
    this.api.deletePipeline(row.id).pipe(
      catchError(() => {
        this.notify.error(messages.deleteFailed);
        return EMPTY;
      }),
      finalize(() => this.finishAction(row.id)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => {
      this.notify.success(messages.deleted(row.name));
      this.refresh();
    });
  }

  download(row: ExportPipelineRow): void {
    if (row.status !== 'COMPLETED' || !this.beginAction(row.id)) return;
    forkJoin({metadata: this.api.getDownloadUrl(row.id), blob: this.api.downloadFile(row.id)}).pipe(
      // A browser download failure is handled by the same recoverable error path as HTTP failures.
      tap(({metadata, blob}) => {
        const url = URL.createObjectURL(blob);
        const anchor = this.document.createElement('a');
        try {
          anchor.href = url;
          anchor.download = metadata.filename;
          this.document.body.append(anchor);
          anchor.click();
        } finally {
          anchor.remove();
          setTimeout(() => URL.revokeObjectURL(url), 0);
        }
      }),
      catchError(() => {
        this.notify.error(messages.downloadFailed);
        return EMPTY;
      }),
      finalize(() => this.finishAction(row.id)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  private beginAction(id: number): boolean {
    if (this.pendingIds().includes(id)) return false;
    this.pendingIds.update(ids => [...ids, id]);
    return true;
  }

  private finishAction(id: number): void {
    this.pendingIds.update(ids => ids.filter(pendingId => pendingId !== id));
  }
}

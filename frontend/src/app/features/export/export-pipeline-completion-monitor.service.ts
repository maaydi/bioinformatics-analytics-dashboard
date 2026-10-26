import {isPlatformBrowser} from '@angular/common';
import {inject, Injectable, PLATFORM_ID} from '@angular/core';
import {catchError, EMPTY, filter, Observable, switchMap, timer} from 'rxjs';
import {ExportPipeline, ExportStatus} from '@core/models/export-pipeline.model';
import {ExportPipelineService} from './export-pipeline.service';

const COMPLETION_POLL_INTERVAL_MS = 3_000;

/**
 * Watches the authenticated user's recent pipelines independently of the export page.
 * The first observation is only a baseline so historical completed exports do not produce
 * notifications; notifications are emitted only for QUEUED/RUNNING → COMPLETED transitions.
 */
@Injectable({providedIn: 'root'})
export class ExportPipelineCompletionMonitor {
  private readonly api = inject(ExportPipelineService);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly STORAGE_KEY = 'exportPipelineId';
  private previousStatus: ExportStatus | null = null;

  readonly completedPipeline$: Observable<ExportPipeline> = !isPlatformBrowser(this.platformId)
    ? EMPTY
    : timer(0, COMPLETION_POLL_INTERVAL_MS).pipe(
      switchMap(() => {
        const idStr = sessionStorage.getItem(this.STORAGE_KEY);
        if (!idStr) {
          return EMPTY;
        }
        return this.api.getPipeline(Number(idStr)).pipe(
          catchError(() => EMPTY)
        );
      }),
      filter(pipeline => this.isNewlyCompleted(pipeline))
    );

  private isNewlyCompleted(pipeline: ExportPipeline): boolean {
    const prev = this.previousStatus;
    this.previousStatus = pipeline.status;
    const isNewlyCompleted =
      pipeline.status === 'COMPLETED' &&
      (prev === 'QUEUED' || prev === 'RUNNING');

    if (isNewlyCompleted && isPlatformBrowser(this.platformId)) {
      sessionStorage.removeItem(this.STORAGE_KEY);
    }

    return isNewlyCompleted;
  }
}


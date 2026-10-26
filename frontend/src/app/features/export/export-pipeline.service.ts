import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {environment} from '@env/environment';
import {
  DownloadUrl,
  ExportFieldSchema,
  ExportJobStatus,
  ExportPipeline,
  ExportPipelineCreateRequest,
  ExportStatus
} from '@core/models/export-pipeline.model';
import {catchError, from, Observable, switchMap, throwError} from 'rxjs';
import {PagedResponse} from '@core/models/paged-response.model';


@Injectable({providedIn: 'root'})
export class ExportPipelineService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/v1/exports`;


  createPipeline(request: ExportPipelineCreateRequest): Observable<ExportPipeline> {
    return this.http.post<ExportPipeline>(`${this.baseUrl}/pipelines`, request);
  }

  listPipelines(status?: ExportStatus, page?: number, size?: number): Observable<PagedResponse<ExportPipeline>> {
    let httpParams: Record<string, string | number> = {};
    if (status) {
      httpParams['status'] = String(status);
    }
    httpParams['page'] = page ?? 0;
    httpParams['size'] = size ?? 20;
    return this.http.get<PagedResponse<ExportPipeline>>(`${this.baseUrl}/pipelines`, {
      params: httpParams,
    });

  }

  getPipeline(id: number): Observable<ExportPipeline> {
    return this.http.get<ExportPipeline>(`${this.baseUrl}/pipelines/${id}`);
  }

  getStatus(id: number): Observable<ExportJobStatus> {
    return this.http.get<ExportJobStatus>(`${this.baseUrl}/pipelines/${id}/status`);
  }

  getDownloadUrl(id: number): Observable<DownloadUrl> {
    return this.http.get<DownloadUrl>(`${this.baseUrl}/pipelines/${id}/download`);

  }

  downloadFile(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/pipelines/${id}/download-file`, {responseType: 'blob'}).pipe(
      catchError((error: unknown) => {
        if (!(error instanceof HttpErrorResponse) || !(error.error instanceof Blob)) {
          return throwError(() => error);
        }
        return from(error.error.text()).pipe(switchMap(text => {
          let body: unknown;
          try {
            body = JSON.parse(text) as unknown;
          } catch {
            return throwError(() => error);
          }
          return throwError(() => new HttpErrorResponse({
            error: body, headers: error.headers, status: error.status,
            statusText: '', url: error.url ?? undefined,
          }));
        }));
      }),
    );
  }

  retryPipeline(id: number): Observable<ExportPipeline> {
    return this.http.post<ExportPipeline>(`${this.baseUrl}/pipelines/${id}/retry`, null);
  }

  deletePipeline(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/pipelines/${id}`);
  }

  getAvailableFields(): Observable<ExportFieldSchema[]> {
    return this.http.get<ExportFieldSchema[]>(`${this.baseUrl}/fields`);
  }
}

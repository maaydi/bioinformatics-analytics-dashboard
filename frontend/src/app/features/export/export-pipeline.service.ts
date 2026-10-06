import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
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
    return this.http.get<Blob>(`${this.baseUrl}/pipelines/${id}/download-file`)
      .pipe(catchError((err) => {
          if (err.error instanceof Blob) {
            return from(err.error.text()).pipe(
              switchMap((errorText) => {
                try {
                  err.error = JSON.parse(errorText as string);
                } catch (e) {
                  err.error = {message: 'An unexpected error occurred during export.'};
                }
                return throwError(() => err);
              })
            );
          }
          return throwError(() => err);
        }
      ));
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

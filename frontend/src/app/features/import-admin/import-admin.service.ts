import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {ImportJobCreated, ImportJobProgress, ImportJobSummary} from '@core/models/import.model';
import {PagedResponse} from '@core/models/paged-response.model';
import {environment} from '@env/environment';

/**
 * Service for import admin API calls.
 *
 * @see documentation/api-contract.md §3 — Import Admin Endpoints
 */
@Injectable({ providedIn: 'root' })
export class ImportAdminService {

  private readonly http    = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/admin/import`;

  triggerImport(file: File, strategy = 'OVERWRITE'): Observable<ImportJobCreated> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('strategy', strategy);
    return this.http.post<ImportJobCreated>(`${this.baseUrl}/uniprot`, formData);
  }

  listImportJobs(page = 0, size = 20): Observable<PagedResponse<ImportJobSummary>> {
    return this.http.get<PagedResponse<ImportJobSummary>>(`${this.baseUrl}/status`, {
      params: { page, size },
    });
  }

  getJobProgress(jobId: string): Observable<ImportJobProgress> {
    return this.http.get<ImportJobProgress>(`${this.baseUrl}/status/${jobId}`);
  }
}

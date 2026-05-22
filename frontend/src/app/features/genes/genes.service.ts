import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {ProteinDetail, ProteinSummary} from '../../core/models/protein.model';
import {PagedResponse} from '../../core/models/paged-response.model';
import {GeneFilterSnapshot} from '../../core/models/saved-filter.model';
import {environment} from '../../../environments/environment';

/**
 * Service for gene/protein API calls.
 *
 * Covers:
 * - GET  /api/genes                  — list
 * - POST /api/genes/search            — search + filter
 * - GET  /api/genes/{id}              — detail
 * - POST /api/genes/export-csv        — CSV download
 *
 * @see documentation/api-contract.md §1 — Gene / Protein Endpoints
 */
@Injectable({ providedIn: 'root' })
export class GenesService {

  private readonly http    = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/genes`;

  listGenes(page = 0, size = 50, sort = 'id', direction = 'asc'): Observable<PagedResponse<ProteinSummary>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', sort)
      .set('direction', direction);
    return this.http.get<PagedResponse<ProteinSummary>>(this.baseUrl, { params });
  }

  searchGenes(filter: GeneFilterSnapshot & { page?: number; size?: number; sort?: string; direction?: string }):
      Observable<PagedResponse<ProteinSummary>> {
    return this.http.post<PagedResponse<ProteinSummary>>(`${this.baseUrl}/search`, filter);
  }

  getGeneById(id: number): Observable<ProteinDetail> {
    return this.http.get<ProteinDetail>(`${this.baseUrl}/${id}`);
  }

  exportCsv(filter: GeneFilterSnapshot): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/export-csv`, filter, { responseType: 'blob' });
  }

  loadKeywords(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/keywords`);
  }
}

import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {ProteinDetail, ProteinSummary} from '@core/models/protein.model';
import {PagedResponse} from '@core/models/paged-response.model';
import {GeneFilterPageSort, GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {environment} from '@env/environment';

/**
 * Data access service for gene/protein endpoints.
 *
 * All methods return cold observables and perform no local state mutation.
 */
@Injectable({providedIn: 'root'})
export class GenesService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/genes`;

  /**
   * Fetches a paginated list of genes.
   * @param page Zero-based page index.
   * @param size Number of records per page.
   * @param sort Backend field name used for sorting.
   * @param direction Sort direction.
   */
  listGenes(page = 0, size = 50, sort = 'id', direction = 'asc'): Observable<PagedResponse<ProteinSummary>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', sort)
      .set('direction', direction);
    return this.http.get<PagedResponse<ProteinSummary>>(this.baseUrl, {params});
  }

  /**
   * Searches genes using server-side filters and paging/sorting options.
   * @param filter Filter payload, optionally including page, size, sort, and direction.
   */
  searchGenes(filter: GeneFilterSnapshot & GeneFilterPageSort):
    Observable<PagedResponse<ProteinSummary>> {
    return this.http.post<PagedResponse<ProteinSummary>>(`${this.baseUrl}/search`, filter);
  }

  /**
   * Fetches the detailed representation of one gene/protein record.
   * @param id Gene identifier.
   */
  getById(id: number): Observable<ProteinDetail> {
    return this.http.get<ProteinDetail>(`${this.baseUrl}/${id}`);
  }

  /**
   * Exports the current filtered result set as CSV.
   * @param filter Filter payload applied to the export.
   */
  exportCsv(filter: GeneFilterSnapshot): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/export-csv`, filter, {responseType: 'blob'});
  }

  /** Loads keyword suggestions for the keywords filter control. */
  loadKeywords(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/keywords`);
  }
}

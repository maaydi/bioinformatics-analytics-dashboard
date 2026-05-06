import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {CreateSavedFilterRequest, SavedFilter} from '@core/models/saved-filter.model';
import {environment} from '@env/environment';

/**
 * Service for saved filter API calls.
 *
 * @see documentation/api-contract.md §4 — Saved Filters Endpoints
 */
@Injectable({ providedIn: 'root' })
export class SavedFiltersService {

  private readonly http    = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/saved-filters`;

  listSavedFilters(): Observable<SavedFilter[]> {
    return this.http.get<SavedFilter[]>(this.baseUrl);
  }

  createSavedFilter(request: CreateSavedFilterRequest): Observable<SavedFilter> {
    return this.http.post<SavedFilter>(this.baseUrl, request);
  }

  deleteSavedFilter(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SavedFilter, CreateSavedFilterRequest } from '../../core/models/saved-filter.model';
import { environment } from '../../../environments/environment';

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

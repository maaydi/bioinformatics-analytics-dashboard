import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '@env/environment';

@Injectable({providedIn: 'root'})
export class AutoCompleteService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/autocomplete`;

  /** Loads suggestions for the given field based on user input. */
  getSuggestion(field: string, query: string): Observable<string[]> {
    return this.http.get<string[]>(this.baseUrl, {params: {field, query}});
  }
}

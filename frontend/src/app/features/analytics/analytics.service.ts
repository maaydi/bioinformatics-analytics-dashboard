import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {
  DashboardKpis,
  EvidenceLevelItem,
  KeywordFrequencyItem,
  LengthHistogramBucket,
  OrganismCount,
  ReviewedRatioItem,
} from '@core/models/analytics.model';
import {environment} from '@env/environment';
import {AnalyticsProvider} from '@shared/components/analytics/analytics-provider';

/**
 * Service for analytics chart data.
 * Same like Dashboard service but using Filters to get metrics
 * Wraps GET /api/analytics/filters/* endpoints.
 *
 * @see documentation/api-contract.md §2 — Analytics Endpoints
 */
@Injectable({providedIn: 'root'})
export class AnalyticsService extends AnalyticsProvider {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/analytics/filters`; // TODO add backend controller

  getDashboardKpis(): Observable<DashboardKpis> {
    return this.http.get<DashboardKpis>(`${this.baseUrl}/dashboard-kpis`);
  }

  getLengthHistogram(): Observable<LengthHistogramBucket[]> {
    return this.http.get<LengthHistogramBucket[]>(`${this.baseUrl}/length-histogram`);
  }

  getByOrganism(limit = 50): Observable<OrganismCount[]> {
    return this.http.get<OrganismCount[]>(`${this.baseUrl}/by-organism`, {params: {limit}});
  }

  getReviewedRatio(): Observable<ReviewedRatioItem[]> {
    return this.http.get<ReviewedRatioItem[]>(`${this.baseUrl}/reviewed-ratio`);
  }

  getEvidenceLevels(): Observable<EvidenceLevelItem[]> {
    return this.http.get<EvidenceLevelItem[]>(`${this.baseUrl}/evidence-levels`);
  }

  getKeywordFrequency(limit = 100): Observable<KeywordFrequencyItem[]> {
    return this.http.get<KeywordFrequencyItem[]>(`${this.baseUrl}/keyword-frequency`, {params: {limit}});
  }
}

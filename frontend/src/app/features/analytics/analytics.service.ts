import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {
  CompareRequest,
  CompareResponse,
  DashboardKpis,
  EvidenceLevelItem,
  KeywordFrequencyItem,
  LengthHistogramBucket,
  OrganismCount,
  ProteinLengthWeightCount,
  ReviewedRatioItem,
} from '@core/models/analytics.model';
import {environment} from '@env/environment';
import {AnalyticsProvider} from '@shared/components/analytics/analytics-provider';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';

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
  private readonly baseUrl = `${environment.apiBaseUrl}/analytics/filters`;

  getDashboardKpis(filter: GeneFilterSnapshot): Observable<DashboardKpis> {
    return this.http.post<DashboardKpis>(`${this.baseUrl}/dashboard-kpis`, filter);
  }

  getLengthHistogram(filter: GeneFilterSnapshot): Observable<LengthHistogramBucket[]> {
    return this.http.post<LengthHistogramBucket[]>(`${this.baseUrl}/length-histogram`, filter);
  }

  getByOrganism(limit = 50, filter: GeneFilterSnapshot): Observable<OrganismCount[]> {
    return this.http.post<OrganismCount[]>(`${this.baseUrl}/by-organism`, filter, {
      params: {limit},
    });
  }

  getReviewedRatio(filter: GeneFilterSnapshot): Observable<ReviewedRatioItem[]> {
    return this.http.post<ReviewedRatioItem[]>(`${this.baseUrl}/reviewed-ratio`, filter);
  }

  getEvidenceLevels(filter: GeneFilterSnapshot): Observable<EvidenceLevelItem[]> {
    return this.http.post<EvidenceLevelItem[]>(`${this.baseUrl}/evidence-levels`, filter);
  }

  getKeywordFrequency(limit = 100, filter: GeneFilterSnapshot): Observable<KeywordFrequencyItem[]> {
    return this.http.post<KeywordFrequencyItem[]>(`${this.baseUrl}/keyword-frequency`, filter, {
      params: {limit},
    });
  }

  getProteinLengthWeightCount(filter?: GeneFilterSnapshot): Observable<ProteinLengthWeightCount[]> {
    return this.http.post<ProteinLengthWeightCount[]>(`${this.baseUrl}/length-weight`, filter);
  }

  compare(request: CompareRequest): Observable<CompareResponse> {
    return this.http.post<CompareResponse>(`${this.baseUrl}/compare`, request);
  }
}

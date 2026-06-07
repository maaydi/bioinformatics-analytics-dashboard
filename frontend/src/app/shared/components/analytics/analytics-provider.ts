import {Observable} from 'rxjs';
import {
  DashboardKpis,
  EvidenceLevelItem,
  KeywordFrequencyItem,
  LengthHistogramBucket,
  OrganismCount,
  ReviewedRatioItem
} from '@core/models/analytics.model';

export abstract class AnalyticsProvider {

  abstract getDashboardKpis(): Observable<DashboardKpis> ;

  abstract getLengthHistogram(): Observable<LengthHistogramBucket[]> ;

  abstract getByOrganism(limit?: number): Observable<OrganismCount[]> ;

  abstract getReviewedRatio(): Observable<ReviewedRatioItem[]> ;

  abstract getEvidenceLevels(): Observable<EvidenceLevelItem[]> ;

  abstract getKeywordFrequency(limit?: number): Observable<KeywordFrequencyItem[]> ;
}

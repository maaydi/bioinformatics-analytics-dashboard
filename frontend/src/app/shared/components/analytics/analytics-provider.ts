import {Observable} from 'rxjs';
import {
  DashboardKpis,
  EvidenceLevelItem,
  KeywordFrequencyItem,
  LengthHistogramBucket,
  OrganismCount,
  ReviewedRatioItem
} from '@core/models/analytics.model';
import {GeneSearchRequest} from '@core/models/saved-filter.model';

export abstract class AnalyticsProvider {

  abstract getDashboardKpis(filter?: GeneSearchRequest): Observable<DashboardKpis> ;

  abstract getLengthHistogram(filter?: GeneSearchRequest): Observable<LengthHistogramBucket[]> ;

  abstract getByOrganism(limit?: number, filter?: GeneSearchRequest): Observable<OrganismCount[]> ;

  abstract getReviewedRatio(filter?: GeneSearchRequest): Observable<ReviewedRatioItem[]> ;

  abstract getEvidenceLevels(filter?: GeneSearchRequest): Observable<EvidenceLevelItem[]> ;

  abstract getKeywordFrequency(limit?: number, filter?: GeneSearchRequest): Observable<KeywordFrequencyItem[]> ;
}

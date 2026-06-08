import {Observable} from 'rxjs';
import {
  DashboardKpis,
  EvidenceLevelItem,
  KeywordFrequencyItem,
  LengthHistogramBucket,
  OrganismCount,
  ReviewedRatioItem
} from '@core/models/analytics.model';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';

export abstract class AnalyticsProvider {

  abstract getDashboardKpis(filter?: GeneFilterSnapshot): Observable<DashboardKpis> ;

  abstract getLengthHistogram(filter?: GeneFilterSnapshot): Observable<LengthHistogramBucket[]> ;

  abstract getByOrganism(limit?: number, filter?: GeneFilterSnapshot): Observable<OrganismCount[]> ;

  abstract getReviewedRatio(filter?: GeneFilterSnapshot): Observable<ReviewedRatioItem[]> ;

  abstract getEvidenceLevels(filter?: GeneFilterSnapshot): Observable<EvidenceLevelItem[]> ;

  abstract getKeywordFrequency(limit?: number, filter?: GeneFilterSnapshot): Observable<KeywordFrequencyItem[]> ;
}

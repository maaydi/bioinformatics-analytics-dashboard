/**
 * TypeScript models for analytics chart API responses.
 *
 * Schemas defined in documentation/api-contract.md §2 — Analytics Endpoints.
 * All field names must match the JSON contract exactly.
 */

import {GeneFilterPageable} from '@core/models/saved-filter.model';

/** GET /api/analytics/dashboard-kpis */
export interface DashboardKpis {
  totalProteins: number;
  reviewedCount: number;
  unreviewedCount: number;
  organismCount: number;
  taxonCount: number;
  avgLength: number;
  avgMolecularWeight: number;
  minLength: number;
  maxLength: number;
}

/** GET /api/analytics/length-histogram — one bucket entry */
export interface LengthHistogramBucket {
  bucket: number;
  rangeMin: number;
  rangeMax: number;
  count: number;
}

/** GET /api/analytics/by-organism — one organism row */
export interface OrganismCount {
  organismName: string;
  taxid: number;
  total: number;
  reviewedCount: number;
  unreviewedCount: number;
  avgLength: number;
}

/** GET /api/analytics/reviewed-ratio */
export interface ReviewedRatioItem {
  reviewed: boolean;
  count: number;
}

/** GET /api/analytics/evidence-levels */
export interface EvidenceLevelItem {
  evidenceLevel: number;
  label: string;
  count: number;
}

/** GET /api/analytics/keyword-frequency */
export interface KeywordFrequencyItem {
  keyword: string;
  count: number;
}

/** GET /api/analytics/length-weight */
export interface ProteinLengthWeightCount {
  length: number;
  moleculeWeight: number;
  count: number;
}


export interface CompareRequest {
  setA: GeneFilterPageable;
  setB: GeneFilterPageable;
}

export interface AnalyticsSubset {
  count: number;
  avgLength: number;
  reviewedCount: number;
  reviewedRatio: number;
  lengthDistribution: LengthHistogramBucket[];
  evidenceDistribution: EvidenceLevelItem[];
}

export interface CompareResponse {
  subsetA: AnalyticsSubset;
  subsetB: AnalyticsSubset;
}

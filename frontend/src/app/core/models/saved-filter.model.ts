/**
 * TypeScript models for saved filter API.
 *
 * Schemas defined in documentation/api-contract.md §4 — Saved Filters Endpoints.
 */

export interface SavedFilter {
  id:         number;
  name:       string;
  filterJson: GeneFilterSnapshot;
  createdAt:  string;
}

export interface CreateSavedFilterRequest {
  name:       string;
  filterJson: GeneFilterSnapshot;
}

/**
 * The filter state that is serialized and stored.
 * Fields mirror the POST /api/genes/search request body.
 */
export interface GeneFilterSnapshot {
  globalSearch?:        string | null;
  accession?:           string | null;
  entryName?:           string | null;
  geneNamePrimary?:     string | null;
  proteinFullName?:     string | null;
  reviewed?:            boolean | null;
  organism?:            string | null;
  taxid?:               number | null;
  lineage?:             string | null;
  lengthMin?:           number | null;
  lengthMax?:           number | null;
  molecularWeightMin?:  number | null;
  molecularWeightMax?:  number | null;
  evidenceLevels?:      number[] | null;
  keywords?:            string[] | null;
  goTermId?:            string | null;
  goAspect?:            'P' | 'F' | 'C' | null;
  featureType?:         string | null;
  crossRefSource?:      string | null;
}

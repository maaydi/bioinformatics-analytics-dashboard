/**
 * TypeScript models for saved filter API.
 *
 * Schemas defined in documentation/api-contract.md §4 — Saved Filters Endpoints.
 */
import {EvidenceLevel} from '@core/models/protein.model';
import {FormControl} from '@angular/forms';

export interface SavedFilter {
  id: number;
  name: string;
  filterJson: GeneFilterSnapshot;
  createdAt: string;
}

export interface CreateSavedFilterRequest {
  name: string;
  filterJson: GeneFilterSnapshot;
}

export interface GeneFilterPageSort {
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'asc' | 'desc';
}

/**
 * The filter state that is serialized and stored.
 * Fields mirror the POST /api/genes/search request body.
 */
export interface GeneFilterSnapshot {
  globalSearch?: string | null;
  accession?: string | null;
  entryName?: string | null;
  geneNamePrimary?: string | null;
  proteinFullName?: string | null;
  reviewed?: boolean | null;
  organism?: string | null;
  taxid?: number | null;
  lineage?: string | null;
  lengthMin?: number | null;
  lengthMax?: number | null;
  molecularWeightMin?: number | null;
  molecularWeightMax?: number | null;
  evidenceLevels?: EvidenceLevel[] | null;
  keywords?: string[] | null;
  goTermId?: string | null;
  goAspect?: 'P' | 'F' | 'C' | null;
  featureType?: string | null;
  crossRefSource?: string | null;
}


export interface GeneFilterFormValue {
  globalSearch: string | null;
  accession: string | null;
  entryName: string | null;
  geneNamePrimary: string | null;
  proteinFullName: string | null;
  reviewed: boolean | null;
  organism: string | null;
  taxid: number | null;
  lineage: string | null;
  length: { min: number | null; max: number | null } | null;
  molecularWeight: { min: number | null; max: number | null } | null;
  evidenceLevels: EvidenceLevel[] | null;
  keywords: string[] | null;
  goTermId: string | null;
  goAspect: 'P' | 'F' | 'C' | null;
  featureType: string | null;
  crossRefSource: string | null;
}

export type GeneFilterFormControls = {
  [K in keyof GeneFilterFormValue]: FormControl<GeneFilterFormValue[K]>;
};

export type GeneFilterPageable = GeneFilterSnapshot & GeneFilterPageSort


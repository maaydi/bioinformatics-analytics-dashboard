/**
 * TypeScript models for protein/gene API responses.
 *
 * Schemas defined in documentation/api-contract.md — Shared Schemas.
 * ALL field names must match the JSON contract exactly (camelCase).
 * Any divergence between this file and api-contract.md is a defect.
 */

/** Used in paginated list responses. Schema: api-contract.md → ProteinSummary */
export interface ProteinSummary {
  id: number;
  accession: string;
  entryName: string;
  proteinFullName: string | null;
  geneNamePrimary: string | null;
  organismName: string;
  taxid: number;
  reviewed: boolean;
  length: number;
  molecularWeight: number | null;
  evidenceLevel: EvidenceLevel;
  keywords: string[];
}

/** Full protein detail — extends ProteinSummary. Schema: api-contract.md → ProteinDetail */
export interface ProteinDetail extends ProteinSummary {
  proteinShortName: string | null;
  proteinEcNumber: string | null;
  geneNameSynonyms: string[];
  geneOrfNames: string[];
  geneOrderedLocus: string[];
  organismCommonName: string | null;
  lineage: string[];
  integratedDate: string | null;
  sequenceDate: string | null;
  updatedDate: string | null;
  sequenceVersion: number | null;
  entryVersion: number | null;
  molecularWeight: number | null;
  sequenceChecksum: string | null;
  sequence: string | null;
  features: FeatureItem[];
  goTerms: GoTermItem[];
  crossReferences: CrossReferenceItem[];
  comments: CommentItem[];
  publications: PublicationItem[];
  hostOrganisms: HostOrganismItem[];
}

export interface FeatureItem {
  type: string;
  startPos: number | null;
  endPos: number | null;
  note: string | null;
  featureId: string | null;
}

export interface GoTermItem {
  goId: string;
  aspect: 'P' | 'F' | 'C';
  description: string;
  evidenceCode: string | null;
}

export interface CrossReferenceItem {
  source: string;
  identifier: string;
  secondaryId: string | null;
  tertiaryInfo: string | null;
}

export interface CommentItem {
  type: string;
  text: string;
}

export interface PublicationItem {
  refNumber: number;
  pubmedId: string | null;
  doi: string | null;
  authors: string | null;
  title: string | null;
  journal: string | null;
}

export interface HostOrganismItem {
  taxid: number;
  name: string;
}

/** Evidence level codes 1–5. See documentation/glossary.md */
export const EVIDENCE_LEVELS = [1, 2, 3, 4, 5] as const;

export type EvidenceLevel = typeof EVIDENCE_LEVELS[number];

export const EVIDENCE_LEVEL_LABELS: Record<EvidenceLevel, string> = {
  1: 'Protein level',
  2: 'Transcript level',
  3: 'Homology',
  4: 'Predicted',
  5: 'Uncertain',
};

export const MAX_GLOBAL_SEARCH_LENGTH: number = 200;
export const MAX_ACCESSION_LENGTH = 20;
export const MAX_GENE_NAME_PRIMARY_LENGTH = 100;
export const MAX_ORGANISM_LENGTH = 300;
export const MAX_KEYWORDS_COUNT = 10;
export const MAX_KEYWORD_LENGTH = 100;

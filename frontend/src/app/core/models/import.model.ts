/**
 * TypeScript models for import admin API.
 *
 * Schemas defined in documentation/api-contract.md §3 — Import Admin Endpoints.
 */

export type ImportStatus = 'RUNNING' | 'COMPLETED' | 'FAILED';

/** Returned by POST /api/admin/import/uniprot (202 Accepted) */
export interface ImportJobCreated {
  id: string;
  status:    ImportStatus;
  createdAt: string;
}

/** Item in paginated list — GET /api/admin/import/status */
export interface ImportJobSummary {
  id:           string;
  status:       ImportStatus;
  fileName:     string | null;
  entryCount:   number | null;
  durationMs:   number | null;
  createdAt:    string;
  completedAt:  string | null;
  errorMessage: string | null;
}

/** Real-time progress — GET /api/admin/import/status/{jobId} */
export interface ImportJobProgress {
  id:                string;
  status:            ImportStatus;
  fileName:          string | null;
  recordsProcessed:  number;
  totalEstimated:    number | null;
  progressPercent:   number;
  elapsedMs:         number;
  errorMessage:      string | null;
}

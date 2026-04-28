import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Import Admin page — Epic 1 (US-1, US-2, US-3). ADMIN only.
 *
 * Features:
 * - File upload form (.dat / .tsv, max 2 GB)
 * - Strategy selector (OVERWRITE)
 * - Submit → POST /api/admin/import/uniprot → 202 Accepted
 * - Progress bar (polls GET /api/admin/import/status/{jobId} every 5 s)
 * - Import job history table (GET /api/admin/import/status)
 *
 * Error handling:
 * - 409 Conflict: "An import is already running"
 * - 413 Payload Too Large: file > 2 GB
 * - 422: unsupported file type
 *
 * TODO: implement in ticket IMPORT-001
 */
@Component({
  selector: 'app-import-admin',
  standalone: true,
  imports: [CommonModule],
  template: `<h1>Import Admin — TODO</h1>`,
})
export class ImportAdminComponent {}

import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Saved Filters page — Epic 7 (US-20, US-21).
 *
 * Features:
 * - List saved filter sets (GET /api/saved-filters)
 * - Click a saved filter → applies to Gene Explorer table
 * - Delete a saved filter (DELETE /api/saved-filters/{id})
 *
 * TODO: implement in ticket FILTER-001
 */
@Component({
  selector: 'app-saved-filters',
  imports: [],
  templateUrl: './saved-filters.component.html',
  styleUrl: './saved-filters.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SavedFiltersComponent {}

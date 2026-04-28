import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Analytics page — Epic 4 full view.
 *
 * Interactive charts:
 * - Protein length histogram (US-11)
 * - Evidence level pie chart (US-12)
 * - Chart-to-table drill-down (US-13)
 * - Dual-subset comparison (US-14)
 * - Proteins by organism bar chart
 * - Reviewed/unreviewed ratio
 * - Keyword frequency chart
 * - Length vs Molecular Weight scatter
 *
 * All chart data from GET /api/analytics/* (materialized views, ≤ 500 ms).
 *
 * TODO: implement in ticket ANALYTICS-001
 */
@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule],
  template: `<h1>Analytics — TODO</h1>`,
})
export class AnalyticsComponent {}

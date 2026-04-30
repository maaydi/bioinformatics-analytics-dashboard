import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { DashboardService } from './dashboard.service';

/**
 * Dashboard page — Epic 4 (US-11 to US-14 partial).
 *
 * Displays:
 * - KPI cards (total proteins, reviewed count, organism count, avg length)
 * - Reviewed vs Unreviewed ratio chart
 * - Proteins by Organism bar chart
 * - Evidence Level pie chart
 * - Protein Length Histogram
 *
 * All data sourced from GET /api/analytics/* (materialized views).
 * Response time target: ≤ 500 ms (NFR §12.1).
 *
 * TODO: implement in ticket DASH-001
 */
@Component({
  selector: 'app-dashboard',
  imports: [MatCardModule, LoadingSpinnerComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);

  ngOnInit(): void {
    // TODO: load KPIs and chart data
  }
}

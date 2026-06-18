import {ChangeDetectionStrategy, Component, input} from '@angular/core';
import {MatCardModule} from '@angular/material/card';

export interface DashboardKpiViewModel {
  readonly title: string;
  readonly label: string;
  readonly value: string;
  readonly unit?: string;
}
@Component({
  selector: 'app-dashboard-kpi-card',
  imports: [MatCardModule],
  templateUrl: './dashboard-kpi-card.component.html',
  styleUrl: './dashboard-kpi-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardKpiCardComponent {
  readonly title = input.required<string>();
  readonly label = input.required<string>();
  readonly value = input.required<string>();
  readonly unit = input('');
}


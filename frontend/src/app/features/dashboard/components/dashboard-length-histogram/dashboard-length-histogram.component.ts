import {ChangeDetectionStrategy, Component} from '@angular/core';
import {MatCardModule} from '@angular/material/card';

@Component({
  selector: 'app-dashboard-length-histogram',
  imports: [MatCardModule],
  templateUrl: './dashboard-length-histogram.component.html',
  styleUrl: './dashboard-length-histogram.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardLengthHistogramComponent {
  protected readonly buckets = [32, 45, 58, 74, 65, 48, 34, 22];
}


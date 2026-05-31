import {ChangeDetectionStrategy, Component} from '@angular/core';
import {MatCardModule} from '@angular/material/card';

@Component({
  selector: 'app-dashboard-reviewed-ratio',
  imports: [MatCardModule],
  templateUrl: './dashboard-reviewed-ratio.component.html',
  styleUrl: './dashboard-reviewed-ratio.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardReviewedRatioComponent {
  protected readonly reviewedPercent = 62;
  protected readonly reviewedCount = '312,048';
  protected readonly unreviewedCount = '258,074';
}


import {ChangeDetectionStrategy, Component} from '@angular/core';
import {MatCardModule} from '@angular/material/card';

interface EvidenceLevelView {
  readonly level: string;
  readonly label: string;
  readonly ratioClass: string;
}

@Component({
  selector: 'app-dashboard-evidence-levels',
  imports: [MatCardModule],
  templateUrl: './dashboard-evidence-levels.component.html',
  styleUrl: './dashboard-evidence-levels.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardEvidenceLevelsComponent {
  protected readonly items: ReadonlyArray<EvidenceLevelView> = [
    {level: '1', label: 'Evidence at protein level', ratioClass: 'ratio-l1'},
    {level: '2', label: 'Evidence at transcript level', ratioClass: 'ratio-l2'},
    {level: '3', label: 'Inferred from homology', ratioClass: 'ratio-l3'},
    {level: '4', label: 'Predicted', ratioClass: 'ratio-l4'},
    {level: '5', label: 'Uncertain', ratioClass: 'ratio-l5'},
  ];
}


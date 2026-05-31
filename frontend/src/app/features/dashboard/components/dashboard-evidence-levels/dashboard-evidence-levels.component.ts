import {ChangeDetectionStrategy, Component} from '@angular/core';
import {MatCardModule} from '@angular/material/card';
import {DecimalPipe} from '@angular/common';

interface EvidenceLevelView {
  readonly level: string;
  readonly label: string;
  readonly count: number;
  readonly ratioClass: string;
}

@Component({
  selector: 'app-dashboard-evidence-levels',
  imports: [MatCardModule, DecimalPipe],
  templateUrl: './dashboard-evidence-levels.component.html',
  styleUrl: './dashboard-evidence-levels.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardEvidenceLevelsComponent {
  protected readonly items: ReadonlyArray<EvidenceLevelView> = [
    {level: '1', label: 'Evidence at protein level', count: 184_220, ratioClass: 'ratio-l1'},
    {level: '2', label: 'Evidence at transcript level', count: 151_844, ratioClass: 'ratio-l2'},
    {level: '3', label: 'Inferred from homology', count: 120_972, ratioClass: 'ratio-l3'},
    {level: '4', label: 'Predicted', count: 82_106, ratioClass: 'ratio-l4'},
    {level: '5', label: 'Uncertain', count: 54_991, ratioClass: 'ratio-l5'},
  ];
}

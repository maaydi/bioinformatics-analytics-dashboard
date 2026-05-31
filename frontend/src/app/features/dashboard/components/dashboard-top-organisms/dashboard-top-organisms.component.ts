import {DecimalPipe} from '@angular/common';
import {ChangeDetectionStrategy, Component} from '@angular/core';
import {MatCardModule} from '@angular/material/card';

interface OrganismView {
  readonly name: string;
  readonly count: number;
  readonly ratioClass: string;
}

@Component({
  selector: 'app-dashboard-top-organisms',
  imports: [MatCardModule, DecimalPipe],
  templateUrl: './dashboard-top-organisms.component.html',
  styleUrl: './dashboard-top-organisms.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardTopOrganismsComponent {
  protected readonly organisms: ReadonlyArray<OrganismView> = [
    {name: 'Homo sapiens', count: 82_791, ratioClass: 'ratio-100'},
    {name: 'Mus musculus', count: 67_884, ratioClass: 'ratio-82'},
    {name: 'Escherichia coli (strain K12)', count: 57_760, ratioClass: 'ratio-70'},
    {name: 'Saccharomyces cerevisiae', count: 52_782, ratioClass: 'ratio-64'},
    {name: 'Arabidopsis thaliana', count: 48_657, ratioClass: 'ratio-59'},
    {name: 'Danio rerio', count: 43_825, ratioClass: 'ratio-53'},
    {name: 'Drosophila melanogaster', count: 40_491, ratioClass: 'ratio-49'},
    {name: 'Rattus norvegicus', count: 37_153, ratioClass: 'ratio-45'},
    {name: 'Caenorhabditis elegans', count: 34_697, ratioClass: 'ratio-42'},
    {name: 'Bacillus subtilis', count: 31_413, ratioClass: 'ratio-38'},
  ];
}

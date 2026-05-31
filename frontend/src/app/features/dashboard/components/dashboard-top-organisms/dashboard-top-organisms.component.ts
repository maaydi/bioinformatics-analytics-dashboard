import {ChangeDetectionStrategy, Component} from '@angular/core';
import {MatCardModule} from '@angular/material/card';

interface OrganismView {
  readonly name: string;
  readonly ratioClass: string;
}

@Component({
  selector: 'app-dashboard-top-organisms',
  imports: [MatCardModule],
  templateUrl: './dashboard-top-organisms.component.html',
  styleUrl: './dashboard-top-organisms.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardTopOrganismsComponent {
  protected readonly organisms: ReadonlyArray<OrganismView> = [
    {name: 'Homo sapiens', ratioClass: 'ratio-100'},
    {name: 'Mus musculus', ratioClass: 'ratio-82'},
    {name: 'Escherichia coli (strain K12)', ratioClass: 'ratio-70'},
    {name: 'Saccharomyces cerevisiae', ratioClass: 'ratio-64'},
    {name: 'Arabidopsis thaliana', ratioClass: 'ratio-59'},
    {name: 'Danio rerio', ratioClass: 'ratio-53'},
    {name: 'Drosophila melanogaster', ratioClass: 'ratio-49'},
    {name: 'Rattus norvegicus', ratioClass: 'ratio-45'},
    {name: 'Caenorhabditis elegans', ratioClass: 'ratio-42'},
    {name: 'Bacillus subtilis', ratioClass: 'ratio-38'},
  ];
}


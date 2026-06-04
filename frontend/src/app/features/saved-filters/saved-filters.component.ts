import {ChangeDetectionStrategy, Component, inject, OnInit, signal} from '@angular/core';
import {SavedFilter} from '@core/models/saved-filter.model';
import {SavedFiltersService} from '@features/saved-filters/saved-filters.service';
import {LoadingSpinnerComponent} from '@shared/components/loading-spinner/loading-spinner.component';
import {MatCard} from '@angular/material/card';
import {MatError} from '@angular/material/input';
import {buildFiltersChips} from '@shared/utils/filter-chips-builder';
import {MatChip, MatChipSet} from '@angular/material/chips';
import {MatIcon} from '@angular/material/icon';
import {formatDate} from '@shared/utils/date-formatter';
import {MatButton, MatIconButton} from '@angular/material/button';


const MOCK_SAVED_FILTERS: SavedFilter[] = [
  {
    id: 1,
    name: 'Gènes Humains Révisés (Poids Élevé)',
    createdAt: '2026-01-15T08:30:00Z',
    filterJson: {
      globalSearch: 'kinase',
      reviewed: true,
      organism: 'Homo sapiens',
      taxid: 9606,
      molecularWeightMin: 50000,
      molecularWeightMax: 150000,
      evidenceLevels: [1, 2],
      keywords: ['Phosphorylation', 'Transferase']
    }
  },
  {
    id: 2,
    name: 'Filtre par Composant Cellulaire GO',
    createdAt: '2026-02-20T14:15:00Z',
    filterJson: {
      goTermId: 'GO:0005886',
      goAspect: 'C',
      lengthMin: 100,
      lengthMax: 500,
      evidenceLevels: [1]
    }
  },
  {
    id: 3,
    name: 'Recherche par Accession & Lignée',
    createdAt: '2026-03-02T11:05:00Z',
    filterJson: {
      accession: 'P12345',
      entryName: 'A4_HUMAN',
      lineage: 'Eukaryota; Metazoa; Chordata',
      evidenceLevels: [3, 4, 5]
    }
  },
  {
    id: 4,
    name: 'Filtre Vide (Réinitialisation)',
    createdAt: '2026-04-10T16:45:00Z',
    filterJson: {
      globalSearch: null,
      reviewed: null,
      evidenceLevels: null,
      keywords: []
    }
  }
];


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
  imports: [
    LoadingSpinnerComponent,
    MatCard,
    MatError,
    MatChip,
    MatChipSet,
    MatIcon,
    MatButton,
    MatIconButton
  ],
  templateUrl: './saved-filters.component.html',
  styleUrl: './saved-filters.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SavedFiltersComponent implements OnInit {
  loading = signal<boolean>(true);
  filters = signal<SavedFilter[]>([]);
  errors = signal<string | null>(null);
  protected readonly buildFiltersChips = buildFiltersChips;
  protected readonly formatDate = formatDate;
  private readonly service = inject(SavedFiltersService);

  ngOnInit(): void {
    this.service.listSavedFilters().subscribe({
      next: sf => {
        this.filters.set(MOCK_SAVED_FILTERS); // TODO remove it after fix style
        this.errors.set(null);
        this.loading.set(false);
      },
      error: err => {
        this.errors.set('Failed to load saved filters');
        this.loading.set(false);
      }
    });
  }

  protected onApply(filter: SavedFilter) {
    console.log('Apply filter ', filter.name);

  }

  protected onDelete(filter: SavedFilter) {
    console.log('delete button ', filter.name);
  }
}

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
import {Router} from '@angular/router';
import {GenesStore} from '@features/genes/state/filters.store';
import {MatDialog} from '@angular/material/dialog';
import {ConfirmDialogComponent} from '@shared/components/confirm-dialog/confirm-dialog.component';

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
  private readonly router = inject(Router);
  private readonly genesStore = inject(GenesStore);

  constructor(private dialog: MatDialog) {
  }

  ngOnInit(): void {
    this.service.listSavedFilters().subscribe({
      next: sf => {
        this.filters.set(sf);
        this.errors.set(null);
        this.loading.set(false);
      },
      error: _ => {
        this.errors.set('Failed to load saved filters');
        this.loading.set(false);
      }
    });
  }

  promptDelete(filter: any): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '350px',
      data: {
        title: 'Confirm Deletion',
        message: 'Are you sure you want to delete this filter ?',
        confirmLabel: 'Delete',
        cancelLabel: 'Cancel'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.onDelete(filter);
      }
    });
  }

  protected onApply(filter: SavedFilter) {
    const snapshot = filter.filterJson;
    this.genesStore.setActiveFilters(snapshot);
    void this.router.navigate(['/genes']);

  }

  private onDelete(filter: SavedFilter) {
    this.service.deleteSavedFilter(filter.id).subscribe({
      next: () => {
        this.filters.update((currentFilters) =>
          currentFilters.filter(f => f.id !== filter.id)
        );
      },
      error: _ => this.errors.set('Failed to delete Filter ' + filter.name)
    });
  }

}

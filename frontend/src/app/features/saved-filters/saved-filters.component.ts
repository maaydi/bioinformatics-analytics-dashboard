import {afterNextRender, ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
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
import {NotificationService} from '@shared/directive/notification.service';
import {MatPaginator, PageEvent} from '@angular/material/paginator';

/**
 * SavedFiltersComponent — Manage persisted gene filter snapshots.
 *
 * Responsibilities:
 * - Load and display list of saved filter sets
 * - Apply a saved filter to the Gene Explorer
 * - Delete a saved filter with confirmation
 * - Handle loading, error, and empty states
 *
 * State signals:
 * - loading: API request in flight
 * - filters: Loaded saved filter list
 * - errors: User-facing error message
 *
 * Integration:
 * - Calls SavedFiltersService for CRUD operations
 * - Updates GenesStore to apply selected filter snapshot
 * - Uses MatDialog for delete confirmation
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
    MatIconButton,
    MatPaginator,
  ],
  templateUrl: './saved-filters.component.html',
  styleUrl: './saved-filters.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SavedFiltersComponent {
  /** True while loading saved filters from API. */
  loading = signal<boolean>(true);

  /** Array of user's saved filter sets. */
  filters = signal<SavedFilter[]>([]);

  /** User-facing error message, or null if no error. */
  errors = signal<string | null>(null);

  totalFilters = signal<number>(0);
  pageSize = signal<number>(5);
  pageIndex = signal<number>(0);

  protected readonly buildFiltersChips = buildFiltersChips;
  protected readonly formatDate = formatDate;
  private readonly service = inject(SavedFiltersService);
  private readonly router = inject(Router);
  private readonly genesStore = inject(GenesStore);
  private readonly notify = inject(NotificationService);

  /**
   * Load saved filters on component init.
   */
  constructor(private dialog: MatDialog) {
    afterNextRender(() => {
      this.loadFilters();
    });
  }


  /**
   * Prompt user for delete confirmation before removing a filter.
   * @param filter — The SavedFilter to delete
   */
  promptDelete(filter: SavedFilter): void {
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

  /**
   * Apply filter to gene explorer and navigate to genes page.
   * @param filter — The SavedFilter to activate
   */
  protected onApply(filter: SavedFilter): void {
    const snapshot = filter.filterJson;
    this.genesStore.setActiveFilters(snapshot);
    void this.router.navigate(['/genes']);
  }

  onPageChange(event: PageEvent) {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadFilters();

  }

  private loadFilters() {
    this.service.listSavedFilters(this.pageIndex(), this.pageSize()).subscribe({
      next: sf => {
        this.filters.set(sf.content);
        this.totalFilters.set(sf.totalElements);
        this.errors.set(null);
        this.loading.set(false);
      },
      error: _ => {
        this.errors.set('Failed to load saved filters');
        this.loading.set(false);
      }
    });
  }

  /**
   * Delete a saved filter and update local state.
   * @param filter — The SavedFilter to delete
   */
  private onDelete(filter: SavedFilter): void {
    this.service.deleteSavedFilter(filter.id).subscribe({
      next: () => {
        this.filters.update((currentFilters) =>
          currentFilters.filter(f => f.id !== filter.id)
        );
        this.notify.success(`Deleted filter "${filter.name}"`, 'Close');
      },
      error: _ => {
        this.errors.set('Failed to delete Filter ' + filter.name);
        this.notify.error(`Failed to delete filter "${filter.name}"`, 'Close');
      }
    });
  }

}

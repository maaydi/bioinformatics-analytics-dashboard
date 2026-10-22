import {afterNextRender, ChangeDetectionStrategy, Component, DestroyRef, inject, signal} from '@angular/core';
import {DatePipe} from '@angular/common';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {catchError, EMPTY, finalize} from 'rxjs';
import {SavedFilter} from '@core/models/saved-filter.model';
import {SavedFiltersService} from '@features/saved-filters/saved-filters.service';
import {MatCard, MatCardContent, MatCardHeader, MatCardSubtitle, MatCardTitle} from '@angular/material/card';
import {MatProgressBar} from '@angular/material/progress-bar';
import {MatTooltip} from '@angular/material/tooltip';
import {buildFiltersChips} from '@shared/utils/filter-chips-builder';
import {MatChip, MatChipSet} from '@angular/material/chips';
import {MatIcon} from '@angular/material/icon';
import {MatButton, MatIconButton} from '@angular/material/button';
import {Router} from '@angular/router';
import {GenesStore} from '@features/genes/state/filters.store';
import {MatDialog} from '@angular/material/dialog';
import {ConfirmDialogComponent} from '@shared/components/confirm-dialog/confirm-dialog.component';
import {NotificationService} from '@shared/directive/notification.service';
import {MatPaginator, PageEvent} from '@angular/material/paginator';
import {SAVED_FILTERS_MESSAGES} from './saved-filters.messages';

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
    DatePipe,
    MatCard,
    MatCardContent,
    MatCardHeader,
    MatCardSubtitle,
    MatCardTitle,
    MatProgressBar,
    MatTooltip,
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
  protected readonly messages = SAVED_FILTERS_MESSAGES;
  /** True while loading saved filters from API. */
  readonly loading = signal<boolean>(true);

  /** Array of user's saved filter sets. */
  readonly filters = signal<readonly SavedFilter[]>([]);

  /** User-facing error message, or null if no error. */
  readonly errors = signal<string | null>(null);

  readonly totalFilters = signal<number>(0);
  readonly pageSize = signal<number>(5);
  readonly pageIndex = signal<number>(0);

  protected readonly buildFiltersChips = buildFiltersChips;
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = inject(MatDialog);
  private readonly service = inject(SavedFiltersService);
  private readonly router = inject(Router);
  private readonly genesStore = inject(GenesStore);
  private readonly notify = inject(NotificationService);

  /**
   * Load saved filters on component init.
   */
  constructor() {
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
        title: this.messages.confirmTitle,
        message: this.messages.confirmMessage,
        confirmLabel: this.messages.confirmDelete,
        cancelLabel: this.messages.cancel
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

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadFilters();

  }

  protected loadFilters(): void {
    this.loading.set(true);
    this.errors.set(null);
    this.service.listSavedFilters(this.pageIndex(), this.pageSize()).pipe(
      catchError(() => {
        this.errors.set(this.messages.loadFailed);
        return EMPTY;
      }),
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.loading.set(false)),
    ).subscribe(sf => {
      this.filters.set(sf.content);
      this.totalFilters.set(sf.totalElements);
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
        this.notify.success(this.messages.deleted(filter.name), this.messages.close);
      },
      error: _ => {
        this.errors.set(this.messages.deleteError(filter.name));
        this.notify.error(this.messages.deleteFailed(filter.name), this.messages.close);
      }
    });
  }

}

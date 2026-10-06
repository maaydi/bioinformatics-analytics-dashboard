import {ChangeDetectionStrategy, Component, computed, input, output} from '@angular/core';
import {GeneSearchRequest} from '@core/models/saved-filter.model';
import {MatChip, MatChipRemove, MatChipSet} from '@angular/material/chips';
import {MatIcon} from '@angular/material/icon';
import {buildFiltersChips, FilterChip} from '@shared/utils/filter-chips-builder';
import {MatButton} from '@angular/material/button';
import {MatProgressSpinner} from '@angular/material/progress-spinner';


@Component({
  selector: 'app-active-filters',
  imports: [
    MatChipSet,
    MatChip,
    MatIcon,
    MatChipRemove,
    MatButton,
    MatProgressSpinner
  ],
  templateUrl: './active-filters.component.html',
  styleUrl: './active-filters.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActiveFiltersComponent {
  readonly exportCsv = output<void>();
  readonly filterRemoved = output<keyof GeneSearchRequest>();
  readonly setChipsCount = output<number>();
  readonly filters = input<GeneSearchRequest | null>(null);
  readonly isExportinProgress = input<boolean>(false);

  readonly filtersChips = computed(() => this.buildChips(this.filters()));


  removeFilter(key: keyof GeneSearchRequest): void {
    this.filterRemoved.emit(key);
  }

  /** Converts non-empty filter fields into display chips. */
  private buildChips(filters: GeneSearchRequest | null): FilterChip[] {
    const chips = buildFiltersChips(filters);
    this.setChipsCount.emit(chips.length);
    return chips;
  }

  protected exportResultCsv() {
    this.exportCsv.emit();
  }
}

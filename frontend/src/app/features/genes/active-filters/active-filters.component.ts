import {ChangeDetectionStrategy, Component, computed, input, output} from '@angular/core';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {MatChip, MatChipRemove, MatChipSet} from '@angular/material/chips';
import {MatIcon} from '@angular/material/icon';
import {buildFiltersChips, FilterChip} from '@shared/utils/filter-chips-builder';


@Component({
  selector: 'app-active-filters',
  imports: [
    MatChipSet,
    MatChip,
    MatIcon,
    MatChipRemove
  ],
  templateUrl: './active-filters.component.html',
  styleUrl: './active-filters.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActiveFiltersComponent {
  readonly filterRemoved = output<keyof GeneFilterSnapshot>();
  readonly setChipsCount = output<number>();
  readonly filters = input<GeneFilterSnapshot | null>(null);

  readonly filtersChips = computed(() => this.buildChips(this.filters()));

  removeFilter(key: keyof GeneFilterSnapshot): void {
    this.filterRemoved.emit(key);
  }

  /** Converts non-empty filter fields into display chips. */
  private buildChips(filters: GeneFilterSnapshot | null): FilterChip[] {
    const chips = buildFiltersChips(filters);
    this.setChipsCount.emit(chips.length);
    return chips;
  }

}

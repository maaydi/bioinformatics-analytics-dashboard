import {ChangeDetectionStrategy, Component, computed, input, output} from '@angular/core';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {MatChip, MatChipRemove, MatChipSet} from '@angular/material/chips';
import {MatIcon} from '@angular/material/icon';

type FilterChip = { key: keyof GeneFilterSnapshot, label: string, value: string };

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

  readonly filtersChips = computed(() => this.buildFiltersChips(this.filters()));

  removeFilter(key: keyof GeneFilterSnapshot): void {
    this.filterRemoved.emit(key);
  }

  /** Converts non-empty filter fields into display chips. */
  private buildFiltersChips(filters: GeneFilterSnapshot | null): FilterChip[] {
    if (!filters) {
      return [];
    }
    const config: Array<{ key: keyof GeneFilterSnapshot; label: string }> = [
      {key: 'globalSearch', label: 'Search'},
      {key: 'accession', label: 'Accession'},
      {key: 'entryName', label: 'Entry'},
      {key: 'geneNamePrimary', label: 'Gene '},
      {key: 'proteinFullName', label: 'Protein'},
      {key: 'reviewed', label: 'Reviewed'},
      {key: 'organism', label: 'Organism'},
      {key: 'taxid', label: 'TaxID'},
      {key: 'lineage', label: 'Lineage'},
      {key: 'lengthMin', label: 'Length Min'},
      {key: 'lengthMax', label: 'Length Max'},
      {key: 'molecularWeightMin', label: 'Weight Min'},
      {key: 'molecularWeightMax', label: 'Weight Max'},
      {key: 'evidenceLevels', label: 'Evidence'},
      {key: 'keywords', label: 'Keywords'},
      {key: 'goTermId', label: 'Go ID'},
      {key: 'goAspect', label: 'Go Aspect'},
      {key: 'featureType', label: 'Feature'},
      {key: 'crossRefSource', label: 'CrossRef'},

    ];
    const chips: FilterChip[] = [];
    for (const item of config) {
      const rawValue = filters[item.key];
      if (rawValue === null || rawValue === undefined || rawValue === '') {
        continue;
      }
      if (Array.isArray(rawValue)) {
        if (rawValue.length === 0) {
          continue;
        }
        chips.push({key: item.key, label: item.label, value: rawValue.join(', ')});
        continue;
      }
      if (item.key === 'reviewed') {
        chips.push({key: item.key, label: item.label, value: rawValue ? 'Yes' : 'No'});
        continue;
      }
      chips.push({key: item.key, label: item.label, value: String(rawValue)});
    }
    this.setChipsCount.emit(chips.length);
    return chips;
  }


}

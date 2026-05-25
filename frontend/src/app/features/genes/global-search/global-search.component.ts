import {ChangeDetectionStrategy, Component, DestroyRef, inject, input, linkedSignal, output} from '@angular/core';
import {MatFormField, MatInput, MatSuffix} from '@angular/material/input';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {MatIcon} from '@angular/material/icon';
import {MatTooltip} from '@angular/material/tooltip';

@Component({
  selector: 'app-global-search',
  imports: [
    MatFormField,
    MatInput,
    MatIcon,
    MatSuffix,
    MatTooltip
  ],
  templateUrl: './global-search.component.html',
  styleUrl: './global-search.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GlobalSearchComponent {
  readonly filters = input<GeneFilterSnapshot | null>(null);
  readonly filterChange = output<GeneFilterSnapshot>();

  readonly globalSearchValue = linkedSignal(() => this.filters()?.globalSearch ?? '');

  private readonly destroyRef = inject(DestroyRef);
  private debounceTimerId: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.destroyRef.onDestroy(() => {
      if (this.debounceTimerId !== null) {
        clearTimeout(this.debounceTimerId);
      }
    });

  }

  onSearchInput(event: Event): void {
    const target = event.target;
    if (!(target instanceof HTMLInputElement)) {
      return;
    }
    const value = target.value;
    this.globalSearchValue.set(value);
    this.emitDebouncedFilterChange(value);
  }

  private emitDebouncedFilterChange(globalSearch: string): void {
    if (this.debounceTimerId !== null) {
      clearTimeout(this.debounceTimerId);
    }
    this.debounceTimerId = setTimeout(() => {
      this.filterChange.emit({
        ...(this.filters() ?? {}),
        globalSearch,
      });
    }, 300);
  }

}

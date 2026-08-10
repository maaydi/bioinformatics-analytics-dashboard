import {ChangeDetectionStrategy, Component, DestroyRef, inject, input, linkedSignal, output} from '@angular/core';
import {MatFormField, MatInput, MatSuffix} from '@angular/material/input';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {MatIcon} from '@angular/material/icon';
import {MatTooltip} from '@angular/material/tooltip';
import {MAX_GLOBAL_SEARCH_LENGTH} from '@core/models/protein.model';
import {MatButton} from '@angular/material/button';
import {DataProviderService} from '@core/provider/data-provider.service';

@Component({
  selector: 'app-global-search',
  imports: [
    MatFormField,
    MatInput,
    MatIcon,
    MatSuffix,
    MatTooltip,
    MatButton
  ],
  templateUrl: './global-search.component.html',
  styleUrl: './global-search.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GlobalSearchComponent {
  protected readonly MAX_GLOBAL_SEARCH_LEN: number = MAX_GLOBAL_SEARCH_LENGTH;
  readonly filters = input<GeneFilterSnapshot | null>(null);

  readonly filterChange = output<GeneFilterSnapshot>();

  readonly globalSearchValue = linkedSignal(() => this.filters()?.globalSearch ?? '');
  private readonly destroyRef = inject(DestroyRef);

  protected readonly dataProviderService = inject(DataProviderService);
  readonly retrySearch = output<void>();


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
    if (value.length <= this.MAX_GLOBAL_SEARCH_LEN) {
      this.emitDebouncedFilterChange(value);
    } else {
      this.clearDebounce();
    }
  }

  errorMessage(): string {
    return `Your search cannot be longer than ${(this.MAX_GLOBAL_SEARCH_LEN)} characters.
      Please shorten your text and try again.`;
  }

  private emitDebouncedFilterChange(globalSearch: string): void {
    this.clearDebounce();
    this.debounceTimerId = setTimeout(() => {
      this.filterChange.emit({
        ...(this.filters() ?? {}),
        globalSearch,
      });
    }, 300);
  }

  private clearDebounce(): void {
    if (this.debounceTimerId !== null) {
      clearTimeout(this.debounceTimerId);
      this.debounceTimerId = null;
    }
  }

  protected toggleProvider() {
    this.dataProviderService.toggleProvider();
    this.retrySearch.emit();

  }
}

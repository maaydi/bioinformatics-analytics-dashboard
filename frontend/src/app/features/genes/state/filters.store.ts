import {inject} from '@angular/core';
import {patchState, signalStore, withMethods, withState} from '@ngrx/signals';
import {rxMethod} from '@ngrx/signals/rxjs-interop';
import {pipe, switchMap, tap} from 'rxjs';
import {ProteinSummary} from '@core/models/protein.model';
import {PagedResponse} from '@core/models/paged-response.model';
import {GeneFilterSnapshot} from '@core/models/saved-filter.model';
import {GenesService} from '@features/genes/genes.service';
import {tapResponse} from '@ngrx/operators';

export interface FilterState {
  activeFilters: GeneFilterSnapshot | null;
  searchResult: PagedResponse<ProteinSummary> | null;
  onErrorMessage: string | null;
  selectedGene: ProteinSummary | null;
  loading: boolean;
}

const initialState: FilterState = {
  activeFilters: null,
  searchResult: null,
  onErrorMessage: null,
  selectedGene: null,
  loading: false,
};

export const GenesStore = signalStore(
  {providedIn: 'root'},
  withState(initialState),
  withMethods((store, geneService = inject(GenesService)) => ({
    /** Stores the row selected in the results table. */
    selectGeneSummary(protein: ProteinSummary): void {
      patchState(store, {selectedGene: protein});
    },

    /** Clears filters, current result set, selected row, and error state. */
    clearFilters(): void {
      patchState(store, {
        activeFilters: null,
        searchResult: null,
        selectedGene: null,
        onErrorMessage: null,
        loading: false
      });
    },

    /** Runs server-side search and updates loading, result, and error state. */
    searchGene: rxMethod<GeneFilterSnapshot>(
      pipe(
        tap((snapshot) => {
          patchState(store, {
            activeFilters: snapshot,
            selectedGene: null,
            loading: true,
          });
        }),
        switchMap((snapshot) =>
          geneService.searchGenes({
            ...snapshot,
            page: 0,
            size: 20,
            sort: 'id',
            direction: 'asc',
          }).pipe(
            tapResponse({
              next: (result) => patchState(store, {
                searchResult: result,
                onErrorMessage: null,
                loading: false,
              }),
              error: (_) => patchState(store, {
                onErrorMessage: 'Failed to search genes. Please contact the administrator for help.',
                loading: false,
              }),
            })
          )
        )
      )
    ),
  }))
);

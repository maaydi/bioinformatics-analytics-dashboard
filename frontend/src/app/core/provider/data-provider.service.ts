import {effect, inject, Injectable, PLATFORM_ID, signal} from '@angular/core';
import {isPlatformBrowser} from '@angular/common';

export type DataProvider = 'postgres' | 'uniprotKb';

@Injectable({
  providedIn: 'root',
})
export class DataProviderService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly STORAGE_KEY = 'gene-provider';

  #currentProvider = signal<DataProvider>('postgres');

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      const savedProvider = localStorage.getItem(this.STORAGE_KEY) as DataProvider;
      if (savedProvider) {
        this.setProvider(savedProvider);
      }
    }

    effect(() => {
      const activeProvider = this.#currentProvider();
      if (isPlatformBrowser(this.platformId)) {
        localStorage.setItem(this.STORAGE_KEY, activeProvider);
      }
    });
  }

  setProvider(provider: DataProvider): void {
    this.#currentProvider.set(provider);
  }

  toggleProvider(): void {
    this.#currentProvider.update((c) => (c === 'postgres' ? 'uniprotKb' : 'postgres'));
  }

  getProvider(): DataProvider {
    return this.#currentProvider();
  }
}

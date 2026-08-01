import {Injectable, signal} from '@angular/core';


export type DataProvider = 'postgres' | 'uniprotKb';

@Injectable({
  providedIn: 'root'
})
export class DataProviderService {
  readonly currentProvider = signal<DataProvider>('postgres');

  setProvider(provider: DataProvider): void {
    this.currentProvider.set(provider);
  }

  toggleProvider(): void {
    this.currentProvider.update((c) => c === 'postgres' ? 'uniprotKb' : 'postgres');
  }

  getProvider(): DataProvider {
    return this.currentProvider();
  }
}

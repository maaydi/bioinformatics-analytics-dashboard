import {DOCUMENT, effect, inject, Injectable, signal} from '@angular/core';

export type Theme = 'light' | 'dark';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly STORAGE_KEY = 'app-theme';

  #theme = signal<Theme>((localStorage.getItem(this.STORAGE_KEY) as Theme) || 'light');

  constructor() {
    effect(() => {
      const activeTheme = this.#theme();
      localStorage.setItem(this.STORAGE_KEY, activeTheme);
      const rootElement = this.document.documentElement;
      if (activeTheme === 'dark') {
        rootElement.classList.add('dark-theme');
        rootElement.classList.remove('light-theme');
      } else {
        rootElement.classList.add('light-theme');
        rootElement.classList.remove('dark-theme');
      }
    });
  }

  toggle(): void {
    this.#theme.update((theme) => (theme === 'light' ? 'dark' : 'light'));
  }

  currentTheme(): Theme {
    return this.#theme();
  }

}

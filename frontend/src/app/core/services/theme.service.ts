import {DOCUMENT, effect, inject, Injectable, PLATFORM_ID, signal} from '@angular/core';
import {isPlatformBrowser} from '@angular/common';

export type Theme = 'light' | 'dark';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly STORAGE_KEY = 'app-theme';

  #theme = signal<Theme>('light');

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      const savedTheme = localStorage.getItem(this.STORAGE_KEY) as Theme;
      if (savedTheme) {
        this.#theme.set(savedTheme);
      }
    }

    effect(() => {
      const activeTheme = this.#theme();

      if (isPlatformBrowser(this.platformId)) {
        localStorage.setItem(this.STORAGE_KEY, activeTheme);
        const rootElement = this.document.documentElement;
        if (activeTheme === 'dark') {
          rootElement.classList.add('dark-theme');
          rootElement.classList.remove('light-theme');
        } else {
          rootElement.classList.add('light-theme');
          rootElement.classList.remove('dark-theme');
        }
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

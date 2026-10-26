import {afterNextRender, DOCUMENT, effect, inject, Injectable, Injector, PLATFORM_ID, signal} from '@angular/core';
import {isPlatformBrowser} from '@angular/common';

export type Theme = 'light' | 'dark';

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly injector = inject(Injector);
  private readonly STORAGE_KEY = 'app-theme';

  readonly #theme = signal<Theme>('light');
  readonly currentTheme = this.#theme.asReadonly();

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      const savedTheme = localStorage.getItem(this.STORAGE_KEY) as Theme | null;
      if (savedTheme === 'light' || savedTheme === 'dark') {
        this.#theme.set(savedTheme);
      }

      afterNextRender(() => {
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
        }, {injector: this.injector});
      });
    }
  }

  toggle(): void {
    this.#theme.update((theme) => (theme === 'light' ? 'dark' : 'light'));
  }
}

import {TestBed} from '@angular/core/testing';
import {ThemeService} from './theme.service';
import {DOCUMENT} from '@angular/common';

describe('ThemeService', () => {
  let mockDocument: any;

  beforeEach(() => {
    localStorage.clear();
    mockDocument = {
      documentElement: {
        classList: {
          add: vi.fn(),
          remove: vi.fn(),
        },
      },
    };

    TestBed.configureTestingModule({
      providers: [ThemeService, {provide: DOCUMENT, useValue: mockDocument}],
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('should load initial theme from localStorage if available', () => {
    localStorage.setItem('app-theme', 'dark');

    const service = TestBed.inject(ThemeService);

    expect(service.currentTheme()).toBe('dark');
  });

  it('should default to light theme if no theme in localStorage', () => {
    const service = TestBed.inject(ThemeService);
    expect(service.currentTheme()).toBe('light');
  });

  it('should flip theme and update localStorage on toggle()', () => {
    const service = TestBed.inject(ThemeService);
    expect(service.currentTheme()).toBe('light');

    service.toggle();
    expect(service.currentTheme()).toBe('dark');

    TestBed.tick();

    expect(localStorage.getItem('app-theme')).toBe('dark');
    expect(mockDocument.documentElement.classList.add).toHaveBeenCalledWith('dark-theme');
    expect(mockDocument.documentElement.classList.remove).toHaveBeenCalledWith('light-theme');

    service.toggle();
    expect(service.currentTheme()).toBe('light');

    TestBed.tick();

    expect(localStorage.getItem('app-theme')).toBe('light');
    expect(mockDocument.documentElement.classList.add).toHaveBeenCalledWith('light-theme');
    expect(mockDocument.documentElement.classList.remove).toHaveBeenCalledWith('dark-theme');
  });
});

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {DebugElement} from '@angular/core';
import {By} from '@angular/platform-browser';
import {MatIconModule} from '@angular/material/icon';

import {BreadcrumbItem, BreadcrumbsComponent} from './breadcrumbs.component';
import {provideRouter} from '@angular/router';

describe('BreadcrumbsComponent', () => {
  let component: BreadcrumbsComponent;
  let fixture: ComponentFixture<BreadcrumbsComponent>;
  let debugElement: DebugElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BreadcrumbsComponent, MatIconModule],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(BreadcrumbsComponent);
    component = fixture.componentInstance;
    debugElement = fixture.debugElement;
  });

  describe('Rendering', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should render navigation with aria-label="breadcrumb"', () => {
      fixture.componentRef.setInput('items', []);
      fixture.detectChanges();

      const nav = debugElement.query(By.css('[aria-label="breadcrumb"]'));
      expect(nav).toBeTruthy();
    });

    it('should render ordered list for breadcrumb items', () => {
      fixture.componentRef.setInput('items', []);
      fixture.detectChanges();

      const list = debugElement.query(By.css('.breadcrumbs-list'));
      expect(list.nativeElement.tagName.toLowerCase()).toBe('ol');
    });

    it('should not render items when array is empty', () => {
      fixture.componentRef.setInput('items', []);
      fixture.detectChanges();

      const items = debugElement.queryAll(By.css('.breadcrumb-item'));
      expect(items.length).toBe(0);
    });
  });

  describe('Single Item', () => {
    it('should render a single breadcrumb item as text when isActive is true', () => {
      const items: readonly BreadcrumbItem[] = [
        {label: 'Home', isActive: true},
      ];
      fixture.componentRef.setInput('items', items);
      fixture.detectChanges();

      const text = debugElement.query(By.css('.breadcrumb-text'));
      expect(text).toBeTruthy();
      expect(text.nativeElement.textContent.trim()).toBe('Home');
    });

    it('should render a single breadcrumb item as link when isActive is false and routerLink provided', () => {
      const items: readonly BreadcrumbItem[] = [
        {label: 'Home', routerLink: ['/'], isActive: false},
      ];
      fixture.componentRef.setInput('items', items);
      fixture.detectChanges();

      const link = debugElement.query(By.css('.breadcrumb-link'));
      expect(link).toBeTruthy();
      expect(link.nativeElement.textContent.trim()).toBe('Home');
    });

    it('should not render separator for single item', () => {
      const items: readonly BreadcrumbItem[] = [
        {label: 'Home', isActive: true},
      ];
      fixture.componentRef.setInput('items', items);
      fixture.detectChanges();

      const separators = debugElement.queryAll(By.css('.breadcrumb-separator'));
      expect(separators.length).toBe(0);
    });
  });

  describe('Multiple Items', () => {
    it('should render multiple breadcrumb items correctly', () => {
      const items: readonly BreadcrumbItem[] = [
        {label: 'Home', routerLink: ['/']},
        {label: 'Genes', routerLink: ['/genes']},
        {label: 'Pro50024', isActive: true},
      ];
      fixture.componentRef.setInput('items', items);
      fixture.detectChanges();

      const breadcrumbItems = debugElement.queryAll(By.css('.breadcrumb-item'));
      expect(breadcrumbItems.length).toBe(3);
    });

    it('should render separators between items (n-1 separators for n items)', () => {
      const items: readonly BreadcrumbItem[] = [
        {label: 'Home', routerLink: ['/']},
        {label: 'Genes', routerLink: ['/genes']},
        {label: 'Pro50024', isActive: true},
      ];
      fixture.componentRef.setInput('items', items);
      fixture.detectChanges();

      const separators = debugElement.queryAll(By.css('.breadcrumb-separator'));
      expect(separators.length).toBe(2);
    });

    it('should render separators with aria-hidden="true"', () => {
      const items: readonly BreadcrumbItem[] = [
        {label: 'Home', routerLink: ['/']},
        {label: 'Genes', routerLink: ['/genes']},
      ];
      fixture.componentRef.setInput('items', items);
      fixture.detectChanges();

      const separator = debugElement.query(By.css('.breadcrumb-separator'));
      expect(separator.nativeElement.getAttribute('aria-hidden')).toBe('true');
    });

    it('should render active item last with aria-current="page"', () => {
      const items: readonly BreadcrumbItem[] = [
        {label: 'Home', routerLink: ['/']},
        {label: 'Genes', routerLink: ['/genes']},
        {label: 'Pro50024', isActive: true},
      ];
      fixture.componentRef.setInput('items', items);
      fixture.detectChanges();

      const activeItem = debugElement.query(By.css('[aria-current="page"]'));
      expect(activeItem).toBeTruthy();
      expect(activeItem.nativeElement.textContent.trim()).toBe('Pro50024');
    });
  });

  describe('Accessibility', () => {
    it('should have role="navigation" on container', () => {
      fixture.componentRef.setInput('items', [
        {label: 'Home', isActive: true},
      ]);
      fixture.detectChanges();

      const nav = debugElement.query(By.css('[role="navigation"]'));
      expect(nav).toBeTruthy();
    });

    it('should have aria-label="breadcrumb" on navigation', () => {
      fixture.componentRef.setInput('items', [
        {label: 'Home', isActive: true},
      ]);
      fixture.detectChanges();

      const nav = debugElement.query(By.css('nav'));
      expect(nav.nativeElement.getAttribute('aria-label')).toBe('breadcrumb');
    });

    it('should have aria-current="page" on active breadcrumb', () => {
      const items: readonly BreadcrumbItem[] = [
        {label: 'Home', routerLink: ['/']},
        {label: 'Current Page', isActive: true},
      ];
      fixture.componentRef.setInput('items', items);
      fixture.detectChanges();

      const activeItem = debugElement.query(By.css('[aria-current="page"]'));
      expect(activeItem).toBeTruthy();
    });

    it('should hide separator icons with aria-hidden', () => {
      const items: readonly BreadcrumbItem[] = [
        {label: 'Home', routerLink: ['/']},
        {label: 'Genes', routerLink: ['/genes']},
      ];
      fixture.componentRef.setInput('items', items);
      fixture.detectChanges();

      const icon = debugElement.query(By.css('.separator-icon'));
      expect(icon.nativeElement.getAttribute('aria-hidden')).toBe('true');
    });

    it('should have proper link styling for router links', () => {
      const items: readonly BreadcrumbItem[] = [
        {label: 'Home', routerLink: ['/']},
      ];
      fixture.componentRef.setInput('items', items);
      fixture.detectChanges();

      const link = debugElement.query(By.css('.breadcrumb-link'));
      expect(link).toBeTruthy();
      expect(link.nativeElement.tagName.toLowerCase()).toBe('a');
    });
  });

  describe('Reactivity', () => {
    it('should update when items signal changes', () => {
      const initialItems: readonly BreadcrumbItem[] = [
        {label: 'Home', isActive: true},
      ];
      fixture.componentRef.setInput('items', initialItems);
      fixture.detectChanges();

      let items = debugElement.queryAll(By.css('.breadcrumb-item'));
      expect(items.length).toBe(1);

      const updatedItems: readonly BreadcrumbItem[] = [
        {label: 'Home', routerLink: ['/']},
        {label: 'Genes', isActive: true},
      ];
      fixture.componentRef.setInput('items', updatedItems);
      fixture.detectChanges();

      items = debugElement.queryAll(By.css('.breadcrumb-item'));
      expect(items.length).toBe(2);
    });

    it('should track items by label for optimal rendering', () => {
      const items: readonly BreadcrumbItem[] = [
        {label: 'Home', routerLink: ['/']},
        {label: 'Genes', isActive: true},
      ];
      fixture.componentRef.setInput('items', items);
      fixture.detectChanges();

      const trackFn = component.trackByLabel;
      expect(trackFn(0, items[0])).toBe('Home');
      expect(trackFn(1, items[1])).toBe('Genes');
    });
  });

  describe('Design System Compliance', () => {
    it('should apply design system classes', () => {
      fixture.componentRef.setInput('items', [
        {label: 'Home', isActive: true},
      ]);
      fixture.detectChanges();

      expect(debugElement.query(By.css('.breadcrumbs-container'))).toBeTruthy();
      expect(debugElement.query(By.css('.breadcrumbs-list'))).toBeTruthy();
      expect(debugElement.query(By.css('.breadcrumb-item'))).toBeTruthy();
    });

    it('should use Material icon for separator', () => {
      const items: readonly BreadcrumbItem[] = [
        {label: 'Home', routerLink: ['/']},
        {label: 'Genes', isActive: true},
      ];
      fixture.componentRef.setInput('items', items);
      fixture.detectChanges();

      const icon = debugElement.query(By.css('.separator-icon'));
      expect(icon.nativeElement.textContent.trim()).toBe('chevron_right');
    });
  });

  describe('Edge Cases', () => {
    it('should handle breadcrumb with neither routerLink nor isActive', () => {
      const items: readonly BreadcrumbItem[] = [
        {label: 'Home'}, // Neither isActive nor routerLink
      ];
      fixture.componentRef.setInput('items', items);
      fixture.detectChanges();

      const text = debugElement.query(By.css('.breadcrumb-text'));
      expect(text).toBeTruthy();
      expect(text.nativeElement.textContent.trim()).toBe('Home');
    });

    it('should handle very long breadcrumb labels', () => {
      const items: readonly BreadcrumbItem[] = [
        {
          label:
            'This is a very long breadcrumb label that might wrap to multiple lines',
          isActive: true,
        },
      ];
      fixture.componentRef.setInput('items', items);
      fixture.detectChanges();

      const text = debugElement.query(By.css('.breadcrumb-text'));
      expect(text).toBeTruthy();
    });

    it('should handle many breadcrumb items (performance)', () => {
      const items: readonly BreadcrumbItem[] = Array.from(
        {length: 10},
        (_, i) => ({
          label: `Level ${i}`,
          routerLink: [`/level${i}`],
          isActive: i === 9,
        })
      );
      fixture.componentRef.setInput('items', items);
      fixture.detectChanges();

      const breadcrumbItems = debugElement.queryAll(By.css('.breadcrumb-item'));
      expect(breadcrumbItems.length).toBe(10);

      const separators = debugElement.queryAll(By.css('.breadcrumb-separator'));
      expect(separators.length).toBe(9);
    });
  });
});


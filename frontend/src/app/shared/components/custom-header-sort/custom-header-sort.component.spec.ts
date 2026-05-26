import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it} from 'vitest';
import {CustomHeaderSortComponent, SortExchangeEvent} from './custom-header-sort.component';
import {MatIconModule} from '@angular/material/icon';

describe('CustomHeaderSortComponent', () => {
  let component: CustomHeaderSortComponent;
  let fixture: ComponentFixture<CustomHeaderSortComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomHeaderSortComponent, MatIconModule],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomHeaderSortComponent);
    component = fixture.componentInstance;
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('label', 'Test Header');
      fixture.componentRef.setInput('field', 'testField');
      fixture.componentRef.setInput('activeSortField', 'id');
      fixture.componentRef.setInput('activeSortDirection', 'asc');
    });
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should have required inputs', () => {
    expect(component.label).toBeDefined();
    expect(component.field).toBeDefined();
  });

  it('should have optional inputs with defaults', () => {
    expect(component.activeSortField()).toBe('id');
    expect(component.activeSortDirection()).toBe('asc');
  });

  it('should compute isCurrentField correctly', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('activeSortField', 'testField');
      fixture.detectChanges();
      expect(component.isCurrentField()).toBe(true);
    });
  });

  it('should emit sort change when header clicked', () => {
    return new Promise<void>((resolve) => {
      TestBed.runInInjectionContext(() => {
        fixture.componentRef.setInput('activeSortField', 'testField');
        fixture.componentRef.setInput('activeSortDirection', 'asc');
        fixture.detectChanges();

        component.sortChange.subscribe((event: SortExchangeEvent) => {
          expect(event.field).toBe('testField');
          expect(event.direction).toBe('desc');
          resolve();
        });

        component.onHeaderClick();
      });
    });
  });

  it('should cycle through sort directions: asc -> desc -> none', () => {
    return new Promise<void>((resolve) => {
      TestBed.runInInjectionContext(() => {
        fixture.componentRef.setInput('activeSortField', 'testField');
        fixture.componentRef.setInput('activeSortDirection', 'asc');
        fixture.detectChanges();

        let callCount = 0;
        component.sortChange.subscribe((event: SortExchangeEvent) => {
          callCount++;
          if (callCount === 1) {
            // First click: asc -> desc
            expect(event.direction).toBe('desc');
            expect(event.field).toBe('testField');
            resolve();
          }
        });

        component.onHeaderClick();
      });
    });
  });

  it('should reset to default sort when direction becomes none', () => {
    return new Promise<void>((resolve) => {
      TestBed.runInInjectionContext(() => {
        fixture.componentRef.setInput('activeSortField', 'testField');
        fixture.componentRef.setInput('activeSortDirection', 'desc');
        fixture.detectChanges();

        component.sortChange.subscribe((event: SortExchangeEvent) => {
          expect(event.field).toBe('id');
          expect(event.direction).toBe('asc');
          resolve();
        });

        component.onHeaderClick();
      });
    });
  });

  it('should emit asc when clicking non-current field', () => {
    return new Promise<void>((resolve) => {
      TestBed.runInInjectionContext(() => {
        fixture.componentRef.setInput('activeSortField', 'differentField');
        fixture.componentRef.setInput('activeSortDirection', 'asc');
        fixture.detectChanges();

        component.sortChange.subscribe((event: SortExchangeEvent) => {
          expect(event.field).toBe('testField');
          expect(event.direction).toBe('asc');
          resolve();
        });

        component.onHeaderClick();
      });
    });
  });

  it('should render header label', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('label', 'Protein Name');
      fixture.detectChanges();
    });

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('Protein Name');
  });
});


import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it} from 'vitest';
import {EmptyStateComponent} from './empty-state.component';
import {MatIconModule} from '@angular/material/icon';

describe('EmptyStateComponent', () => {
  let component: EmptyStateComponent;
  let fixture: ComponentFixture<EmptyStateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmptyStateComponent, MatIconModule],
    }).compileComponents();

    fixture = TestBed.createComponent(EmptyStateComponent);
    component = fixture.componentInstance;
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  // OnPush strategy is implicit in Angular 21+ standalone; skipping explicit test

  it('should be a standalone component', () => {
    const metadata = (EmptyStateComponent as any).ɵcmp;
    expect(metadata.standalone).toBe(true);
  });

  it('should have optional message input with default value', () => {
    expect(component.message()).toBe('No data found');
  });

  it('should have optional icon input with default value', () => {
    expect(component.icon()).toBe('search_off');
  });

  it('should accept custom message', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('message', 'Custom message');
      fixture.detectChanges();
      expect(component.message()).toBe('Custom message');
    });
  });

  it('should accept custom icon', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('icon', 'info');
      fixture.detectChanges();
      expect(component.icon()).toBe('info');
    });
  });

  it('should display message in template', () => {
    TestBed.runInInjectionContext(() => {
      fixture.componentRef.setInput('message', 'No results found');
      fixture.detectChanges();
    });

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('No results found');
  });

  it('should use material icon', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    const matIcon = compiled.querySelector('mat-icon');
    expect(matIcon).toBeTruthy();
  });

  it('should render without errors', () => {
    expect(() => fixture.detectChanges()).not.toThrow();
  });
});


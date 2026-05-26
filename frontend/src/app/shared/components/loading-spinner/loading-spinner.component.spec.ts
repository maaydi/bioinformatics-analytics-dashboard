import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it} from 'vitest';
import {LoadingSpinnerComponent} from './loading-spinner.component';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';

describe('LoadingSpinnerComponent', () => {
  let component: LoadingSpinnerComponent;
  let fixture: ComponentFixture<LoadingSpinnerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoadingSpinnerComponent, MatProgressSpinnerModule],
    }).compileComponents();

    fixture = TestBed.createComponent(LoadingSpinnerComponent);
    component = fixture.componentInstance;
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should be a standalone component', () => {
    const metadata = (LoadingSpinnerComponent as any).ɵcmp;
    expect(metadata.standalone).toBe(true);
  });

  it('should render mat-spinner', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    const spinner = compiled.querySelector('mat-spinner');
    expect(spinner).toBeTruthy();
  });

  it('should have spinner wrapper with accessibility attributes', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement;
    const wrapper = compiled.querySelector('.spinner-wrapper');
    expect(wrapper).toBeTruthy();
    expect(wrapper?.getAttribute('role')).toBe('status');
    expect(wrapper?.getAttribute('aria-label')).toBe('Loading');
  });

  it('should render without errors', () => {
    expect(() => fixture.detectChanges()).not.toThrow();
  });

  it('should satisfy loading state requirement from constitution.md', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('mat-spinner')).toBeTruthy();
  });
});


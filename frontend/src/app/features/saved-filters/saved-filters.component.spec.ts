import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it} from 'vitest';
import {SavedFiltersComponent} from './saved-filters.component';

describe('SavedFiltersComponent', () => {
  let component: SavedFiltersComponent;
  let fixture: ComponentFixture<SavedFiltersComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SavedFiltersComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SavedFiltersComponent);
    component = fixture.componentInstance;
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should render without errors', () => {
    expect(() => fixture.detectChanges()).not.toThrow();
  });
});


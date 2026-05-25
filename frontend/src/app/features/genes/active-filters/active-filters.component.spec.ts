import {ComponentFixture, TestBed} from '@angular/core/testing';

import {ActiveFiltersComponent} from './active-filters.component';

describe('ActiveFilters', () => {
  let component: ActiveFiltersComponent;
  let fixture: ComponentFixture<ActiveFiltersComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ActiveFiltersComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ActiveFiltersComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

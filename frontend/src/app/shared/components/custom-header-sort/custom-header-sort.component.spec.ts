import {ComponentFixture, TestBed} from '@angular/core/testing';

import {CustomHeaderSortComponent} from './custom-header-sort.component';

describe('CustomHeaderSortComponent', () => {
  let component: CustomHeaderSortComponent;
  let fixture: ComponentFixture<CustomHeaderSortComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomHeaderSortComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomHeaderSortComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

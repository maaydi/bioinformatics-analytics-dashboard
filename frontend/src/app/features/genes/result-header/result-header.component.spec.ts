import {ComponentFixture, TestBed} from '@angular/core/testing';

import {ResultHeaderComponent} from './result-header.component';

describe('ResultHeaderComponent', () => {
  let component: ResultHeaderComponent;
  let fixture: ComponentFixture<ResultHeaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResultHeaderComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ResultHeaderComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

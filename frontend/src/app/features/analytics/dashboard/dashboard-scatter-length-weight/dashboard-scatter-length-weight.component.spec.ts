import {ComponentFixture, TestBed} from '@angular/core/testing';

import {DashboardScatterLengthWeightComponent} from './dashboard-scatter-length-weight.component';

describe('DashboardScatterLengthWeightComponent', () => {
  let component: DashboardScatterLengthWeightComponent;
  let fixture: ComponentFixture<DashboardScatterLengthWeightComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardScatterLengthWeightComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardScatterLengthWeightComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

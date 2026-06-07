import {ComponentFixture, TestBed} from '@angular/core/testing';
import {DashboardKpiCardComponent} from './dashboard-kpi-card.component';

describe('DashboardKpiCardComponent', () => {
  let fixture: ComponentFixture<DashboardKpiCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardKpiCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardKpiCardComponent);
  });

  it('should render KPI values with unit when provided', () => {
    fixture.componentRef.setInput('title', 'Avg Len');
    fixture.componentRef.setInput('label', 'Average Length');
    fixture.componentRef.setInput('value', '360');
    fixture.componentRef.setInput('unit', 'AA');
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Avg Len');
    expect(text).toContain('Average Length');
    expect(text).toContain('360');
    expect(text).toContain('AA');
  });

  it('should hide unit when unit input is empty', () => {
    fixture.componentRef.setInput('title', 'Total');
    fixture.componentRef.setInput('label', 'Total proteins');
    fixture.componentRef.setInput('value', '570,000');
    fixture.detectChanges();

    const unitEl = fixture.nativeElement.querySelector('.kpi-unit');
    expect(unitEl).toBeNull();
  });
});


import {ComponentFixture, TestBed} from '@angular/core/testing';

import {CompareEvidenceLevelComponent} from './compare-evidence-level.component';

describe('CompareEvidenceLevelComponent', () => {
  let component: CompareEvidenceLevelComponent;
  let fixture: ComponentFixture<CompareEvidenceLevelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CompareEvidenceLevelComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CompareEvidenceLevelComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('renders its heading, default labels, and empty message', () => {
    const element: HTMLElement = fixture.nativeElement;

    expect(element.querySelector('h2')?.textContent).toBe('Evidence Levels Comparison');
    expect(element.textContent).toContain('Base Filter');
    expect(element.textContent).toContain('Compare Filter');
    expect(element.querySelector('.chart-state')?.textContent).toBe('No comparison data available.');
  });

  it('renders dynamic evidence labels and accessible comparison counts', async () => {
    fixture.componentRef.setInput('primaryData', [{evidenceLevel: 1, label: 'Protein level', count: 1200}]);
    fixture.componentRef.setInput('secondaryData', [{evidenceLevel: 1, label: 'Protein level', count: 20}]);
    fixture.componentRef.setInput('primaryLabel', 'Set A');
    fixture.componentRef.setInput('secondaryLabel', 'Set B');
    await fixture.whenStable();
    const element: HTMLElement = fixture.nativeElement;

    expect(element.querySelector('[role="list"]')?.getAttribute('aria-label')).toBe('Evidence levels comparison');
    expect(element.querySelector('.row-label')?.textContent).toBe('L1 - Protein level');
    expect(element.querySelector('[aria-label="Set A: 1200"]')).not.toBeNull();
    expect(element.querySelector('[aria-label="Set B: 20"]')).not.toBeNull();
    expect(element.querySelector('.count')?.textContent).toBe('1,200');
  });
});

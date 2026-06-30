import {ComponentFixture, TestBed} from '@angular/core/testing';
import {LimitSelectorComponent} from './limit-selector.component';

describe('LimitSelectorComponent', () => {
  let component: LimitSelectorComponent;
  let fixture: ComponentFixture<LimitSelectorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LimitSelectorComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(LimitSelectorComponent);
    component = fixture.componentInstance;

    // Set required inputs
    fixture.componentRef.setInput('min', 5);
    fixture.componentRef.setInput('max', 1000);
    fixture.componentRef.setInput('defaultValue', 10);

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize control with default value and validators', () => {
    expect(component['limitControl'].value).toBe(10);
    expect(component['limitControl'].valid).toBe(true);
  });

  it('should emit limitChange when a valid option is selected', () => {
    const emitSpy = vi.spyOn(component.limitChange, 'emit');
    component['onOptionSelected'](25);
    expect(emitSpy).toHaveBeenCalledWith(25);
  });

  it('should not emit limitChange if control is invalid on option select', () => {
    const emitSpy = vi.spyOn(component.limitChange, 'emit');
    component['limitControl'].setValue(2000); // Invalid based on max(1000)
    // Manually trigger valid state handling for test since setValue itself doesn't lock onOptionSelected
    // Wait, onOptionSelected checks this.limitControl.valid
    component['onOptionSelected'](25);
    expect(emitSpy).not.toHaveBeenCalled();
  });

  it('should emit valid values from input after debounce', async () => {
    const emitSpy = vi.spyOn(component.limitChange, 'emit');

    component['limitControl'].setValue(50);

    // Wait for debounceTime(400)
    await new Promise(resolve => setTimeout(resolve, 450));

    expect(emitSpy).toHaveBeenCalledWith(50);
  });

  it('should compute presets bounded by min and max', () => {
    fixture.componentRef.setInput('min', 25);
    fixture.componentRef.setInput('max', 250);
    fixture.detectChanges();

    expect(component['presets']()).toEqual([25, 50, 100, 250]);
  });
});


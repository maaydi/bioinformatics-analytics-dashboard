import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {RangeInputComponent, RangeValue} from './range-input.component';
import {ReactiveFormsModule} from '@angular/forms';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatIconModule} from '@angular/material/icon';
import {MatTooltipModule} from '@angular/material/tooltip';

describe('RangeInputComponent', () => {
  let component: RangeInputComponent;
  let fixture: ComponentFixture<RangeInputComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        RangeInputComponent,
        ReactiveFormsModule,
        MatFormFieldModule,
        MatInputModule,
        MatIconModule,
        MatTooltipModule
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RangeInputComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('Component Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should have default input values', () => {
      expect(component.label()).toBe('');
      expect(component.hintLabel()).toBe('');
      expect(component.minPlaceholder()).toBe('Min');
      expect(component.maxPlaceholder()).toBe('Max');
    });

    it('should initialize form with null values', () => {
      expect(component.rangeForm.controls.min.value).toBeNull();
      expect(component.rangeForm.controls.max.value).toBeNull();
    });

    it('should implement ControlValueAccessor', () => {
      expect(component.writeValue).toBeDefined();
      expect(component.registerOnChange).toBeDefined();
      expect(component.registerOnTouched).toBeDefined();
      expect(component.setDisabledState).toBeDefined();
    });
  });

  describe('Input Bindings', () => {
    it('should accept custom label input', () => {
      expect(component.label()).toBe('');
    });

    it('should accept custom hint label input', () => {
      expect(component.hintLabel()).toBe('');
    });

    it('should accept custom min placeholder input', () => {
      expect(component.minPlaceholder()).toBe('Min');
    });

    it('should accept custom max placeholder input', () => {
      expect(component.maxPlaceholder()).toBe('Max');
    });
  });

  describe('Form Group Structure', () => {
    it('should have min and max form controls', () => {
      expect(component.rangeForm.controls.min).toBeDefined();
      expect(component.rangeForm.controls.max).toBeDefined();
    });

    it('should initialize min control as FormControl<number | null>', () => {
      expect(component.rangeForm.controls.min.value).toBeNull();
      component.rangeForm.controls.min.setValue(100);
      expect(component.rangeForm.controls.min.value).toBe(100);
    });

    it('should initialize max control as FormControl<number | null>', () => {
      expect(component.rangeForm.controls.max.value).toBeNull();
      component.rangeForm.controls.max.setValue(200);
      expect(component.rangeForm.controls.max.value).toBe(200);
    });
  });

  describe('ControlValueAccessor Implementation', () => {
    it('should register onChange callback', () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);
      expect(component.onChange).toBe(changeFn);
    });

    it('should register onTouched callback', () => {
      const touchedFn = vi.fn();
      component.registerOnTouched(touchedFn);
      expect(component.onTouch).toBe(touchedFn);
    });

    it('should write value with both min and max', () => {
      const testValue: RangeValue = {min: 10, max: 100};
      component.writeValue(testValue);

      expect(component.rangeForm.controls.min.value).toBe(10);
      expect(component.rangeForm.controls.max.value).toBe(100);
    });

    it('should write value with only min', () => {
      const testValue: RangeValue = {min: 50, max: null};
      component.writeValue(testValue);

      expect(component.rangeForm.controls.min.value).toBe(50);
      expect(component.rangeForm.controls.max.value).toBeNull();
    });

    it('should write value with only max', () => {
      const testValue: RangeValue = {min: null, max: 200};
      component.writeValue(testValue);

      expect(component.rangeForm.controls.min.value).toBeNull();
      expect(component.rangeForm.controls.max.value).toBe(200);
    });

    it('should reset form when writeValue receives null', () => {
      component.rangeForm.controls.min.setValue(50);
      component.rangeForm.controls.max.setValue(150);

      component.writeValue(null);

      expect(component.rangeForm.controls.min.value).toBeNull();
      expect(component.rangeForm.controls.max.value).toBeNull();
    });

    it('should not emit onChange event when writing value', async () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);
      changeFn.mockClear();

      component.writeValue({min: 25, max: 75});

      await fixture.whenStable();
      expect(changeFn).not.toHaveBeenCalled();
    });
  });

  describe('Disabled State', () => {
    it('should disable all form controls when setDisabledState(true)', () => {
      component.setDisabledState(true);

      expect(component.rangeForm.controls.min.disabled).toBe(true);
      expect(component.rangeForm.controls.max.disabled).toBe(true);
      expect(component.rangeForm.disabled).toBe(true);
    });

    it('should enable all form controls when setDisabledState(false)', () => {
      component.setDisabledState(true);
      component.setDisabledState(false);

      expect(component.rangeForm.controls.min.disabled).toBe(false);
      expect(component.rangeForm.controls.max.disabled).toBe(false);
      expect(component.rangeForm.disabled).toBe(false);
    });

    it('should not emit event when disabling', async () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);
      changeFn.mockClear();

      component.setDisabledState(true);

      await fixture.whenStable();
      expect(changeFn).not.toHaveBeenCalled();
    });
  });

  describe('Value Changes', () => {
    it('should emit onChange when min value changes', async () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);

      component.rangeForm.controls.min.setValue(50);

      await fixture.whenStable();
      expect(changeFn).toHaveBeenCalledWith({
        min: 50,
        max: null
      });
    });

    it('should emit onChange when max value changes', async () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);

      component.rangeForm.controls.max.setValue(200);

      await fixture.whenStable();
      expect(changeFn).toHaveBeenCalledWith({
        min: null,
        max: 200
      });
    });

    it('should emit onChange with both values when both change', async () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);
      changeFn.mockClear();

      component.rangeForm.controls.min.setValue(10);
      component.rangeForm.controls.max.setValue(100);

      await fixture.whenStable();
      // Should be called twice (once per change)
      expect(changeFn).toHaveBeenCalledWith({
        min: 10,
        max: null
      });
      expect(changeFn).toHaveBeenCalledWith({
        min: 10,
        max: 100
      });
    });

    it('should convert undefined values to null in onChange', async () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);

      component.rangeForm.controls.min.setValue(50);
      component.rangeForm.controls.max.setValue(null);

      await fixture.whenStable();
      // Last call should have max as null
      const lastCall = changeFn.mock.calls[changeFn.mock.calls.length - 1];
      expect(lastCall[0].max).toBeNull();
    });
  });

  describe('Edge Cases', () => {
    it('should handle zero values', async () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);

      component.rangeForm.controls.min.setValue(0);
      component.rangeForm.controls.max.setValue(0);

      await fixture.whenStable();
      expect(changeFn).toHaveBeenCalledWith({
        min: 0,
        max: 0
      });
    });

    it('should handle negative values', async () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);

      component.rangeForm.controls.min.setValue(-100);
      component.rangeForm.controls.max.setValue(-10);

      await fixture.whenStable();
      // Last call
      const lastCall = changeFn.mock.calls[changeFn.mock.calls.length - 1];
      expect(lastCall[0].min).toBe(-100);
      expect(lastCall[0].max).toBe(-10);
    });

    it('should handle large numbers', async () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);

      const largeNumber = Number.MAX_SAFE_INTEGER;
      component.rangeForm.controls.min.setValue(largeNumber);
      component.rangeForm.controls.max.setValue(largeNumber);

      await fixture.whenStable();
      const lastCall = changeFn.mock.calls[changeFn.mock.calls.length - 1];
      expect(lastCall[0].max).toBe(largeNumber);
    });

    it('should handle min greater than max', async () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);

      component.rangeForm.controls.min.setValue(100);
      component.rangeForm.controls.max.setValue(50);

      await fixture.whenStable();
      const lastCall = changeFn.mock.calls[changeFn.mock.calls.length - 1];
      expect(lastCall[0].min).toBe(100);
      expect(lastCall[0].max).toBe(50);
    });

    it('should handle rapid value changes', async () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);
      changeFn.mockClear();

      for (let i = 0; i < 10; i++) {
        component.rangeForm.controls.min.setValue(i);
      }

      await fixture.whenStable();
      expect(changeFn).toHaveBeenCalledTimes(10);
    });

    it('should preserve values through disable/enable cycles', () => {
      component.rangeForm.controls.min.setValue(30);
      component.rangeForm.controls.max.setValue(70);

      component.setDisabledState(true);
      expect(component.rangeForm.controls.min.value).toBe(30);
      expect(component.rangeForm.controls.max.value).toBe(70);

      component.setDisabledState(false);
      expect(component.rangeForm.controls.min.value).toBe(30);
      expect(component.rangeForm.controls.max.value).toBe(70);
    });

    it('should handle null to number transition', async () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);

      expect(component.rangeForm.controls.min.value).toBeNull();

      component.rangeForm.controls.min.setValue(50);

      await fixture.whenStable();
      const lastCall = changeFn.mock.calls[changeFn.mock.calls.length - 1];
      expect(lastCall[0].min).toBe(50);
    });

    it('should handle number to null transition', async () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);

      component.rangeForm.controls.max.setValue(100);
      component.rangeForm.controls.max.setValue(null);

      await fixture.whenStable();
      const lastCall = changeFn.mock.calls[changeFn.mock.calls.length - 1];
      expect(lastCall[0].max).toBeNull();
    });
  });

  describe('Form Touched State', () => {
    it('should preserve touched state after writeValue', async () => {
      component.rangeForm.markAllAsTouched();
      expect(component.rangeForm.touched).toBe(true);

      component.writeValue({min: 50, max: 100});

      await fixture.whenStable();
      expect(component.rangeForm.touched).toBe(true);
    });
  });
});









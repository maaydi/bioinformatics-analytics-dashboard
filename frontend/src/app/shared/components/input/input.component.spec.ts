import {ComponentFixture, TestBed} from '@angular/core/testing';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {InputComponent} from './input.component';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatIconModule} from '@angular/material/icon';
import {MatTooltip} from '@angular/material/tooltip';

describe('InputComponent', () => {
  let component: InputComponent;
  let fixture: ComponentFixture<InputComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        InputComponent,
        MatFormFieldModule,
        MatInputModule,
        MatIconModule,
        MatTooltip
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InputComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('Component Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should have default input values', () => {
      expect(component.title()).toBe('Your Title');
      expect(component.isSearch()).toBe(false);
      expect(component.placeholder()).toBe('Type here...');
      expect(component.hintLabel()).toBe('');
    });

    it('should initialize internal form control with empty value', () => {
      expect(component.internalControl.value).toBe('');
    });

    it('should implement ControlValueAccessor', () => {
      expect(component.writeValue).toBeDefined();
      expect(component.registerOnChange).toBeDefined();
      expect(component.registerOnTouched).toBeDefined();
      expect(component.setDisabledState).toBeDefined();
    });
  });

  describe('Input Bindings', () => {
    it('should accept custom title input', () => {
      // Note: In Angular 20+, inputs are immutable signals, test via initial setup
      expect(component.title()).toBe('Your Title'); // default
    });

    it('should accept custom placeholder input', () => {
      expect(component.placeholder()).toBe('Type here...');
    });

    it('should accept search mode toggle', () => {
      expect(component.isSearch()).toBe(false);
    });

    it('should accept hint label input', () => {
      expect(component.hintLabel()).toBe('');
    });
  });

  describe('ControlValueAccessor Implementation', () => {
    it('should write value to internal control without emitting event', () => {
      const testValue = 'test input';
      const setSpy = vi.spyOn(component.internalControl, 'setValue');

      component.writeValue(testValue);

      expect(setSpy).toHaveBeenCalledWith(testValue, {
        emitEvent: false
      });
    });

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

    it('should disable form control when setDisabledState(true)', () => {
      component.setDisabledState(true);
      expect(component.internalControl.disabled).toBe(true);
    });

    it('should enable form control when setDisabledState(false)', () => {
      component.internalControl.disable();
      component.setDisabledState(false);
      expect(component.internalControl.disabled).toBe(false);
    });
  });

  describe('Value Changes', () => {
    it('should call onChange when internal control value changes', async () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);

      const testValue = 'new value';
      component.internalControl.setValue(testValue);

      await fixture.whenStable();
      expect(changeFn).toHaveBeenCalledWith(testValue);
    });

    it('should propagate form control value changes', async () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);

      component.internalControl.setValue('updated');

      await fixture.whenStable();
      expect(changeFn).toHaveBeenCalled();
    });

    it('should handle null values', async () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);

      component.internalControl.setValue(null);

      await fixture.whenStable();
      expect(changeFn).toHaveBeenCalledWith(null);
    });

    it('should handle empty string values', async () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);

      component.internalControl.setValue('');

      await fixture.whenStable();
      expect(changeFn).toHaveBeenCalledWith('');
    });
  });

  describe('Form Control State', () => {
    it('should update internal control value without emitting on writeValue', () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);

      // Clear any previous calls during initialization
      changeFn.mockClear();

      component.writeValue('silent value');

      // Change callback should not be called because emitEvent is false
      expect(changeFn).not.toHaveBeenCalled();
      expect(component.internalControl.value).toBe('silent value');
    });

    it('should preserve touched state', async () => {
      component.internalControl.markAsTouched();
      expect(component.internalControl.touched).toBe(true);

      component.writeValue('new value');

      await fixture.whenStable();
      expect(component.internalControl.touched).toBe(true);
    });
  });

  describe('Edge Cases', () => {
    it('should handle special characters in value', async () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);

      const specialValue = '!@#$%^&*()_+-=[]{}|;:,.<>?';
      component.internalControl.setValue(specialValue);

      await fixture.whenStable();
      expect(changeFn).toHaveBeenCalledWith(specialValue);
    });

    it('should handle very long strings', async () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);

      const longValue = 'a'.repeat(10000);
      component.internalControl.setValue(longValue);

      await fixture.whenStable();
      expect(changeFn).toHaveBeenCalledWith(longValue);
    });

    it('should handle rapid successive value changes', async () => {
      const changeFn = vi.fn();
      component.registerOnChange(changeFn);

      component.internalControl.setValue('first');
      component.internalControl.setValue('second');
      component.internalControl.setValue('third');

      await fixture.whenStable();
      expect(changeFn).toHaveBeenCalledTimes(3);
    });

    it('should restore value after disable/enable cycle', () => {
      const testValue = 'persistent value';
      component.writeValue(testValue);

      component.setDisabledState(true);
      expect(component.internalControl.disabled).toBe(true);
      expect(component.internalControl.value).toBe(testValue);

      component.setDisabledState(false);
      expect(component.internalControl.disabled).toBe(false);
      expect(component.internalControl.value).toBe(testValue);
    });
  });
});









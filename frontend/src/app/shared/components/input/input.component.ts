import {Component, forwardRef, input} from '@angular/core';
import {ControlValueAccessor, FormControl, NG_VALUE_ACCESSOR, ReactiveFormsModule} from '@angular/forms';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatIconModule} from '@angular/material/icon';
import {MatTooltip} from '@angular/material/tooltip';

@Component({
  selector: 'app-input',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatIconModule, MatTooltip],
  templateUrl: './input.component.html',
  styleUrls: ['./input.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => InputComponent),
      multi: true
    }
  ]
})
/**
 * Generic text input control compatible with reactive forms.
 *
 * Inputs:
 * - `title`: field label.
 * - `isSearch`: enables search-style visual behavior.
 * - `placeholder`: input placeholder text.
 * - `hintLabel`: optional helper text.
 */
export class InputComponent implements ControlValueAccessor {
  title = input<string>('Your Title');
  isSearch = input<boolean>(false);
  placeholder = input<string>('Type here...');
  hintLabel = input<string>('');

  internalControl = new FormControl('');

  constructor() {
    this.internalControl.valueChanges.subscribe(value => {
      this.onChange(value);
    });
  }

  onChange: any = () => {
  };

  onTouch: any = () => {
  };

  /** Writes value from parent form control without re-emitting change events. */
  writeValue(value: string): void {
    this.internalControl.setValue(value, {emitEvent: false});
  }

  /** Registers callback invoked on input value changes. */
  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  /** Registers callback invoked when the control is touched. */
  registerOnTouched(fn: any): void {
    this.onTouch = fn;
  }

  /** Applies disabled state from the parent form control. */
  setDisabledState(isDisabled: boolean): void {
    isDisabled ? this.internalControl.disable() : this.internalControl.enable();
  }
}

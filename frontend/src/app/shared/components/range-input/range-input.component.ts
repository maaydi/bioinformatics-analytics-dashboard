import {Component, forwardRef, input} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ReactiveFormsModule,
  ValidationErrors,
  Validator
} from '@angular/forms';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatIconModule} from '@angular/material/icon';
import {MatTooltipModule} from '@angular/material/tooltip';

export interface RangeValue {
  min: number | null;
  max: number | null;
}

@Component({
  selector: 'app-range-input',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatTooltipModule
  ],
  templateUrl: './range-input.component.html',
  styleUrls: ['./range-input.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RangeInputComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => RangeInputComponent),
      multi: true
    }
  ]
})
/**
 * Numeric min/max range control compatible with reactive forms.
 *
 * Inputs:
 * - `label`: field label.
 * - `hintLabel`: optional helper text.
 * - `minPlaceholder`: placeholder for minimum value.
 * - `maxPlaceholder`: placeholder for maximum value.
 */
export class RangeInputComponent implements ControlValueAccessor, Validator {
  label = input<string>('');
  hintLabel = input<string>('');
  minPlaceholder = input<string>('Min');
  maxPlaceholder = input<string>('Max');

  rangeForm = new FormGroup({
    min: new FormControl<number | null>(null),
    max: new FormControl<number | null>(null)
  });

  constructor() {
    this.rangeForm.valueChanges.subscribe((value) => {
      this.onChange({
        min: value.min ?? null,
        max: value.max ?? null
      });
    });
  }

  onChange: any = () => {
  };

  onTouch: any = () => {
  };


  validate(control: AbstractControl): ValidationErrors | null {
    return this.isMinGreaterThanMax() ? {minGreaterThanMax: true} : null;
  }

  protected isMinGreaterThanMax(): boolean {
    const min = this.rangeForm.get('min')?.value;
    const max = this.rangeForm.get('max')?.value;

    return !!(min && max && min > max);
  }

  /** Writes parent control value into the internal min/max form. */
  writeValue(value: RangeValue | null): void {
    if (value) {
      this.rangeForm.setValue({
        min: value.min,
        max: value.max
      }, {emitEvent: false});
    } else {
      this.rangeForm.reset({}, {emitEvent: false});
    }
  }

  /** Registers callback invoked when min/max values change. */
  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  /** Registers callback invoked when the control is touched. */
  registerOnTouched(fn: any): void {
    this.onTouch = fn;
  }

  /** Enables or disables the internal range form. */
  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.rangeForm.disable({emitEvent: false});
    } else {
      this.rangeForm.enable({emitEvent: false});
    }
  }

}

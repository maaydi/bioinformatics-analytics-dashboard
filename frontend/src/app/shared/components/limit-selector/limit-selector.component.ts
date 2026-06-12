import {Component, computed, DestroyRef, inject, input, OnInit, output} from '@angular/core';
import {FormControl, ReactiveFormsModule, Validators} from '@angular/forms';
import {debounceTime, filter} from 'rxjs/operators';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {MatError, MatFormField, MatInput} from '@angular/material/input';
import {MatAutocomplete, MatAutocompleteTrigger, MatOption} from '@angular/material/autocomplete';

@Component({
  selector: 'app-limit-selector',
  imports: [
    MatFormField,
    MatInput,
    ReactiveFormsModule,
    MatAutocompleteTrigger,
    MatAutocomplete,
    MatOption,
    MatError
  ],
  templateUrl: './limit-selector.component.html',
  styleUrl: './limit-selector.component.scss',
})
export class LimitSelectorComponent implements OnInit {
  readonly min = input.required<number>();
  readonly max = input.required<number>();
  readonly defaultValue = input.required<number>();
  public readonly limitChange = output<number>();
  protected readonly presets = computed(() => {
    const staticPresets = [5, 10, 25, 50, 100, 250, 500, 1000];
    const miv = this.min();
    const mav = this.max();
    return staticPresets.filter(p => p >= miv && p <= mav);
  });
  protected readonly limitControl = new FormControl<number>(10, {nonNullable: true});
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.initializeControl();
    this.setupValueChangesStream();
  }

  protected onOptionSelected(value: number) {
    if (this.limitControl.valid) {
      this.limitChange.emit(value);
    }
  }

  private initializeControl(): void {
    const dv = this.defaultValue();
    this.limitControl.setValue(dv, {emitEvent: false});
    this.limitControl.setValidators([
      Validators.required,
      Validators.min(this.min()),
      Validators.max(this.max()),
    ]);
    this.limitControl.updateValueAndValidity({emitEvent: false});
  }

  private setupValueChangesStream(): void {
    this.limitControl.valueChanges
      .pipe(debounceTime(400),
        filter(() => this.limitControl.valid),
        takeUntilDestroyed(this.destroyRef))
      .subscribe((v) => this.limitChange.emit(v));
  }
}

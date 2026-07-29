import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  forwardRef,
  inject,
  input,
  OnInit,
  signal,
} from '@angular/core';
import {ControlValueAccessor, FormControl, NG_VALUE_ACCESSOR, ReactiveFormsModule,} from '@angular/forms';
import {catchError, debounceTime, distinctUntilChanged, map, startWith, switchMap,} from 'rxjs/operators';
import {takeUntilDestroyed, toObservable} from '@angular/core/rxjs-interop';
import {
  MatAutocomplete,
  MatAutocompleteSelectedEvent,
  MatAutocompleteTrigger,
  MatOption,
} from '@angular/material/autocomplete';
import {MatFormField, MatInput} from '@angular/material/input';
import {MatCheckbox} from '@angular/material/checkbox';
import {AsyncPipe} from '@angular/common';
import {combineLatest, Observable, of} from 'rxjs';
import {HttpErrorResponse} from '@angular/common/http';
import {AutoCompleteService} from './autocomplete.service';

@Component({
  selector: 'app-generic-autocomplete',
  templateUrl: './generic-autocomplete.component.html',
  styleUrl: './generic-autocomplete.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatFormField,
    MatInput,
    ReactiveFormsModule,
    MatAutocompleteTrigger,
    MatAutocomplete,
    MatOption,
    MatCheckbox,
    AsyncPipe,
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => GenericAutocompleteComponent),
      multi: true,
    },
  ],
})
export class GenericAutocompleteComponent implements ControlValueAccessor, OnInit {
  field = input.required<string>();
  multiSelect = input<boolean>(true);
  placeholder = input<string>('Search...');

  protected selectedValue = signal<string | string[] | null>(null);
  protected searchCtrl = new FormControl<string>('', {nonNullable: true});
  protected filteredOptions$!: Observable<string[]>;
  protected isDisabled = false;

  private destroyRef = inject(DestroyRef);
  private autoCompleteService = inject(AutoCompleteService);

  // Convert selectedValue signal to an Observable to combine with search query
  private selected$ = toObservable(this.selectedValue);

  ngOnInit(): void {
    this.initAutoCompleteStream();
  }

  writeValue(value: string | string[] | null): void {
    if (this.multiSelect()) {
      this.selectedValue.set(Array.isArray(value) ? value : value ? [value] : []);
    } else {
      this.selectedValue.set(typeof value === 'string' ? value : null);
      if (typeof value === 'string') {
        this.searchCtrl.setValue(value, {emitEvent: false});
      }
    }
  }

  registerOnChange(fn: (value: string | string[] | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.isDisabled = isDisabled;
    if (isDisabled) {
      this.searchCtrl.disable();
    } else {
      this.searchCtrl.enable();
    }
  }

  protected onTouched: () => void = () => {
  };

  protected isSelected(option: string): boolean {
    const val = this.selectedValue();
    if (this.multiSelect() && Array.isArray(val)) {
      return val.includes(option);
    }
    return val === option;
  }

  protected onOptionSelected(event: MatAutocompleteSelectedEvent): void {
    const value = event.option.value as string;

    if (this.multiSelect()) {
      const current = (this.selectedValue() as string[]) || [];
      const updated = current.includes(value)
        ? current.filter((k) => k !== value)
        : [...current, value];

      this.selectedValue.set(updated);

      // Deselect option visually and clear input so selected elements are shown at top
      setTimeout(() => {
        event.option.deselect();
        this.searchCtrl.setValue('');
      });
    } else {
      this.selectedValue.set(value);
    }

    this.notifyParent();
  }

  protected getHintLabel(): string {
    if (this.multiSelect()) {
      const val = this.selectedValue() as string[];
      return val?.length > 0 ? `${val.length} Selected` : '';
    }
    return '';
  }

  private onChange: (value: string | string[] | null) => void = () => {
  };

  private initAutoCompleteStream(): void {
    const search$ = this.searchCtrl.valueChanges.pipe(
      startWith(this.searchCtrl.value),
      debounceTime(250),
      distinctUntilChanged(),
    );

    this.filteredOptions$ = combineLatest([search$, this.selected$]).pipe(
      takeUntilDestroyed(this.destroyRef),
      switchMap(([query, selected]) =>
        this.autoCompleteService.getSuggestion(this.field(), query).pipe(
          map((suggestions) => {
            // When query is empty in multi-select mode, prepend all selected elements
            if (this.multiSelect() && !query.trim()) {
              const selectedList = Array.isArray(selected) ? selected : [];
              return Array.from(new Set([...selectedList, ...suggestions]));
            }
            return suggestions;
          }),
          catchError((err) => {
            console.error('Failed to retrieve suggestions: ', (err as HttpErrorResponse).message);
            if (this.multiSelect() && !query.trim()) {
              const selectedList = Array.isArray(selected) ? selected : [];
              return of(selectedList);
            }
            return of([]);
          }),
        ),
      ),
    );
  }

  private notifyParent(): void {
    this.onChange(this.selectedValue());
    this.onTouched();
  }
}

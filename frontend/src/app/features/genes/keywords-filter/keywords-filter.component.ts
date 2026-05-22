import {ChangeDetectionStrategy, Component, DestroyRef, forwardRef, inject, OnInit, signal} from '@angular/core';
import {ControlValueAccessor, FormControl, NG_VALUE_ACCESSOR, ReactiveFormsModule} from '@angular/forms';
import {debounceTime, distinctUntilChanged, map, startWith} from 'rxjs/operators';
import {GenesService} from '@features/genes/genes.service';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {
  MatAutocomplete,
  MatAutocompleteSelectedEvent,
  MatAutocompleteTrigger,
  MatOption
} from '@angular/material/autocomplete';
import {MatFormField, MatInput} from '@angular/material/input';
import {MatCheckbox} from '@angular/material/checkbox';
import {AsyncPipe} from '@angular/common';
import {Observable} from 'rxjs';

@Component({
  selector: 'app-keywords-filter',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatFormField,
    MatInput,
    ReactiveFormsModule,
    MatAutocompleteTrigger,
    MatAutocomplete,
    MatOption,
    MatCheckbox,
    AsyncPipe
  ],
  templateUrl: './keywords-filter.component.html',
  styleUrl: './keywords-filter.component.scss',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => KeywordsFilterComponent),
    multi: true
  }]
})
/**
 * Keyword multi-select control with autocomplete suggestions.
 *
 * Behavior:
 * - Implements `ControlValueAccessor` for reactive forms integration.
 * - Loads available keywords from backend once on init.
 * - Emits selected keyword arrays on every toggle.
 */
export class KeywordsFilterComponent implements OnInit, ControlValueAccessor {

  protected selectedKeywords = signal<string[]>([]);
  protected keywordSearchCtrl = new FormControl<string>('', {nonNullable: true});
  protected filteredKeywords$!: Observable<string[]>;
  protected isDisabled = false;
  private destroyRef = inject(DestroyRef);
  private geneService = inject(GenesService);
  private allKeywords: string[] = [];

  ngOnInit(): void {
    this.loadKeywordsFromBackend();
    this.initAutoCompleteStream();
  }

  /** Writes the current form value from parent control state. */
  writeValue(value: string[] | null): void {
    this.selectedKeywords.set(value || []);
  }

  /** Registers callback invoked when selected keywords change. */
  registerOnChange(fn: (value: string[]) => void): void {
    this.onChange = fn;
  }

  /** Registers callback invoked when control is touched. */
  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  /** Enables or disables the search input according to parent form state. */
  setDisabledState(isDisabled: boolean): void {
    this.isDisabled = isDisabled;
    if (isDisabled) {
      this.keywordSearchCtrl.disable();
    } else {
      this.keywordSearchCtrl.enable();
    }
  }

  protected onTouched: () => void = () => {
  };

  protected isSelected(keyword: string): boolean {
    return this.selectedKeywords().includes(keyword);
  }

  /** Adds or removes a keyword and propagates the updated selection. */
  protected toggleKeyword(event: MatAutocompleteSelectedEvent): void {
    const value = event.option.value as string;
    const current = this.selectedKeywords();
    if (current.includes(value)) {
      this.selectedKeywords.set(current.filter(k => k !== value));
    } else {
      this.selectedKeywords.set([...current, value]);
    }
    this.notifyParent();
    // Keep autocomplete open and preserve search text
    setTimeout(() => {
      event.option.deselect();
    });
  }

  /** Returns a compact summary label for selected keywords. */
  protected getSelectedKeywords() {
    const k = this.selectedKeywords();
    return k.length > 0
      ? `${k.length} Selected`
      : '';
  }

  private onChange: (value: string[]) => void = () => {
  };

  private loadKeywordsFromBackend(): void {
    this.geneService.loadKeywords()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(k => this.allKeywords.push(...k));
  }

  /** Builds the autocomplete stream from user input text. */
  private initAutoCompleteStream(): void {
    this.filteredKeywords$ = this.keywordSearchCtrl.valueChanges.pipe(
      debounceTime(150),
      distinctUntilChanged(),
      startWith(''),
      map(value => (typeof value === 'string' ? value : '')),
      map(value => this.filterKeywords(value))
    );
  }

  /** Filters backend keywords by case-insensitive substring match. */
  private filterKeywords(value: string): string[] {
    const v = value.toLowerCase().trim();
    return this.allKeywords
      .filter(k => k.toLowerCase().includes(v))
      .slice(0, 20);
  }

  /** Notifies the parent control about value and touch updates. */
  private notifyParent(): void {
    this.onChange(this.selectedKeywords());
    this.onTouched();
  }
}

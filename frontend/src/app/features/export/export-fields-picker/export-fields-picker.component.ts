import {ChangeDetectionStrategy, Component, computed, forwardRef, input, signal} from '@angular/core';
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from '@angular/forms';
import {MatTooltip} from '@angular/material/tooltip';
import {ExportFieldSchema} from '@core/models/export-pipeline.model';
import {I18nPluralPipe} from '@angular/common';
import {EXPORT_FIELD_SELECTION_MESSAGES} from '@features/export/export-field-selection.messages';

@Component({
  selector: 'app-export-fields-picker',
  imports: [I18nPluralPipe, MatTooltip],
  templateUrl: './export-fields-picker.component.html',
  styleUrl: './export-fields-picker.component.scss',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => ExportFieldsPickerComponent),
    multi: true,
  }],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExportFieldsPickerComponent implements ControlValueAccessor {
  readonly fields = input<readonly ExportFieldSchema[]>([]);

  protected readonly messages = EXPORT_FIELD_SELECTION_MESSAGES;
  protected readonly selectedFields = signal<readonly string[]>([]);
  protected readonly selectedFieldNames = computed(() => new Set(this.selectedFields()));
  protected readonly selectedCount = computed(() =>
    this.fields().filter((field) => this.selectedFieldNames().has(field.fieldName)).length,
  );
  protected readonly disabled = signal(false);

  writeValue(value: string[] | null): void {
    this.selectedFields.set([...(value ?? [])]);
  }

  registerOnChange(fn: (value: string[]) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled.set(isDisabled);
  }

  protected markTouched(): void {
    this.onTouched();
  }

  protected toggleField(fieldName: string): void {
    if (this.disabled()) {
      return;
    }

    const current = this.selectedFields();
    const next = current.includes(fieldName)
      ? current.filter((name) => name !== fieldName)
      : [...current, fieldName];

    this.selectedFields.set(next);
    this.onChange(next);
    this.onTouched();
  }

  protected selectAll(): void {
    if (this.disabled()) {
      return;
    }

    const next = this.fields().map((field) => field.fieldName);
    this.selectedFields.set(next);
    this.onChange(next);
    this.onTouched();
  }

  protected clearAll(): void {
    if (this.disabled()) {
      return;
    }

    this.selectedFields.set([]);
    this.onChange([]);
    this.onTouched();
  }

  private onChange: (value: string[]) => void = () => {
  };

  private onTouched: () => void = () => {
  };
}

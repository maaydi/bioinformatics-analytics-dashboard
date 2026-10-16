import {TestbedHarnessEnvironment} from '@angular/cdk/testing/testbed';
import {ChangeDetectionStrategy, Component, signal} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FormControl, ReactiveFormsModule} from '@angular/forms';
import {MatTooltip} from '@angular/material/tooltip';
import {MatTooltipHarness} from '@angular/material/tooltip/testing';
import {By} from '@angular/platform-browser';
import {beforeEach, describe, expect, it} from 'vitest';
import {ExportFieldSchema} from '@core/models/export-pipeline.model';
import {ExportFieldsPickerComponent} from './export-fields-picker.component';

@Component({
  imports: [ReactiveFormsModule, ExportFieldsPickerComponent],
  template: '<app-export-fields-picker [fields]="fields()" [formControl]="control" />',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
class FieldPickerTestHost {
  readonly control = new FormControl<string[]>(['accession'], {nonNullable: true});
  readonly fields = signal<readonly ExportFieldSchema[]>([
    {fieldName: 'accession', displayName: 'Accession', dataType: 'STRING', description: 'Stable identifier.'},
    {fieldName: 'length', displayName: '', dataType: 'NUMBER', description: 'Number of residues.'},
  ]);
}

describe('ExportFieldsPickerComponent', () => {
  let fixture: ComponentFixture<FieldPickerTestHost>;
  let host: FieldPickerTestHost;

  function elements<T extends Element>(selector: string): T[] {
    return Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<T>(selector));
  }

  function element<T extends Element>(selector: string): T {
    const result = elements<T>(selector)[0];
    if (result === undefined) {
      throw new Error(`Expected element: ${selector}`);
    }
    return result;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({imports: [FieldPickerTestHost]}).compileComponents();
    fixture = TestBed.createComponent(FieldPickerTestHost);
    host = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('groups native checkboxes with a legend rather than listbox roles', () => {
    expect(element<HTMLFieldSetElement>('fieldset').querySelector('legend')?.textContent)
      .toBe('Available export fields');
    expect(elements('[role="listbox"], [role="option"], [aria-multiselectable]')).toHaveLength(0);
    expect(elements<HTMLInputElement>('label input[type="checkbox"]')).toHaveLength(2);
    expect(element<HTMLElement>('[role="status"]').textContent?.trim()).toBe('1 field selected');
  });

  it('renders display names, field-name fallback, and data types without inline descriptions', () => {
    expect(elements<HTMLElement>('.field-name').map((name) => name.textContent)).toEqual(['Accession', 'length']);
    expect(elements<HTMLElement>('.field-type').map((type) => type.textContent)).toEqual(['STRING', 'NUMBER']);
    expect(elements('.field-description')).toHaveLength(0);
    expect(element<HTMLFieldSetElement>('fieldset').textContent).not.toContain('Stable identifier.');
    expect(element<HTMLFieldSetElement>('fieldset').textContent).not.toContain('Number of residues.');
  });

  it('binds each description to its checkbox tooltip', () => {
    const tooltips = fixture.debugElement.queryAll(By.css('input[type="checkbox"]'))
      .map((checkbox) => checkbox.injector.get(MatTooltip));

    expect(tooltips.map((tooltip) => tooltip.message)).toEqual(['Stable identifier.', 'Number of residues.']);
    expect(tooltips.every((tooltip) => !tooltip.disabled)).toBe(true);
  });

  it.each(['hover', 'keyboard focus'] as const)('shows the description on checkbox %s', async (interaction) => {
    const checkbox = element<HTMLInputElement>('input');
    const tooltip = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatTooltipHarness);
    expect(await tooltip.isOpen()).toBe(false);

    if (interaction === 'hover') {
      checkbox.dispatchEvent(new MouseEvent('mouseenter'));
    } else {
      document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Tab', bubbles: true}));
      checkbox.focus();
    }

    await expect.poll(() => tooltip.isOpen()).toBe(true);
    expect(await tooltip.getTooltipText()).toBe('Stable identifier.');
    expect(host.control.value).toEqual(['accession']);
  });

  it('disables tooltips when descriptions become empty', async () => {
    host.fields.update((fields) => fields.map((field) => ({...field, description: ''})));
    await fixture.whenStable();

    const tooltips = fixture.debugElement.queryAll(By.directive(MatTooltip))
      .map((checkbox) => checkbox.injector.get(MatTooltip));
    expect(tooltips).toHaveLength(2);
    expect(tooltips.every((tooltip) => tooltip.disabled && tooltip.message === '')).toBe(true);
    expect(host.control.value).toEqual(['accession']);
  });

  it('reflects programmatic form values without mutating the supplied selection', async () => {
    const selection = ['length'];
    host.control.setValue(selection);
    await fixture.whenStable();

    expect(elements<HTMLInputElement>('input').map((input) => input.checked)).toEqual([false, true]);
    element<HTMLInputElement>('input').click();
    await fixture.whenStable();
    expect(host.control.value).toEqual(['length', 'accession']);
    expect(selection).toEqual(['length']);
    expect(host.control.dirty).toBe(true);
  });

  it('marks the form control touched when focus leaves a checkbox without changing it', async () => {
    expect(host.control.touched).toBe(false);
    element<HTMLInputElement>('input').dispatchEvent(new FocusEvent('focusout', {bubbles: true}));
    await fixture.whenStable();
    expect(host.control.touched).toBe(true);
    expect(host.control.value).toEqual(['accession']);
  });

  it('selects and clears fields through non-submitting bulk actions', async () => {
    expect(elements<HTMLButtonElement>('button').every((button) => button.type === 'button')).toBe(true);
    element<HTMLButtonElement>('button:first-child').click();
    await fixture.whenStable();

    expect(host.control.value).toEqual(['accession', 'length']);
    expect(element<HTMLButtonElement>('button:first-child').disabled).toBe(true);
    expect(element<HTMLElement>('[role="status"]').textContent?.trim()).toBe('2 fields selected');

    element<HTMLButtonElement>('button:last-child').click();
    await fixture.whenStable();
    expect(host.control.value).toEqual([]);
    expect(elements<HTMLInputElement>('input').every((input) => !input.checked)).toBe(true);
    expect(element<HTMLButtonElement>('button:last-child').disabled).toBe(true);
  });

  it('disables all interactions and reacts to re-enabling under OnPush', async () => {
    host.control.disable();
    await fixture.whenStable();

    expect(element<HTMLFieldSetElement>('fieldset').disabled).toBe(true);
    expect(elements<HTMLInputElement>('input').every((input) => input.disabled)).toBe(true);
    expect(elements<HTMLButtonElement>('button').every((button) => button.disabled)).toBe(true);
    element<HTMLInputElement>('input').click();
    element<HTMLButtonElement>('button:last-child').click();
    expect(host.control.value).toEqual(['accession']);

    host.control.enable();
    await fixture.whenStable();
    expect(element<HTMLFieldSetElement>('fieldset').disabled).toBe(false);
    expect(elements<HTMLInputElement>('input').every((input) => !input.disabled)).toBe(true);
    element<HTMLInputElement>('label:last-child input').click();
    await fixture.whenStable();
    expect(host.control.value).toEqual(['accession', 'length']);
  });

  it('shows an empty state without changing the form selection when fields change', async () => {
    host.fields.set([]);
    await fixture.whenStable();

    expect(element<HTMLElement>('.field-picker-empty').textContent).toBe('No fields available.');
    expect(elements('input')).toHaveLength(0);
    expect(element<HTMLButtonElement>('button:first-child').disabled).toBe(true);
    expect(host.control.value).toEqual(['accession']);
  });
});

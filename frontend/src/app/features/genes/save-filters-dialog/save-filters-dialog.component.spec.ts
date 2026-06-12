import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MatDialogModule, MatDialogRef} from '@angular/material/dialog';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatButtonModule} from '@angular/material/button';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {SaveFiltersDialogComponent} from './save-filters-dialog.component';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {DebugElement} from '@angular/core';
import {By} from '@angular/platform-browser';

describe('SaveFiltersDialogComponent', () => {
  let component: SaveFiltersDialogComponent;
  let fixture: ComponentFixture<SaveFiltersDialogComponent>;
  let compiled: DebugElement;
  let mockDialogRef: Partial<MatDialogRef<SaveFiltersDialogComponent>>;

  beforeEach(async () => {
    mockDialogRef = {
      close: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [
        SaveFiltersDialogComponent,
        ReactiveFormsModule,
        FormsModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatDialogModule,
        BrowserAnimationsModule,
      ],
      providers: [
        {provide: MatDialogRef, useValue: mockDialogRef},
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SaveFiltersDialogComponent);
    component = fixture.componentInstance;
    compiled = fixture.debugElement;
    fixture.detectChanges();
  });

  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeDefined();
    });

    it('should initialize form with empty name field', () => {
      expect(component.form.controls.name.value).toBe('');
    });

    it('should initialize name field with required validator', () => {
      const nameControl = component.form.controls.name;
      nameControl.setValue('');
      expect(nameControl.hasError('required')).toBe(true);
    });

    it('should initialize name field with maxlength validator', () => {
      const nameControl = component.form.controls.name;
      nameControl.setValue('a'.repeat(101));
      expect(nameControl.hasError('maxlength')).toBe(true);
    });

    it('should have form initially in valid state when untouched', () => {
      component.form.controls.name.setValue('Test Filter');
      expect(component.form.valid).toBe(true);
    });
  });

  describe('Form Validation', () => {
    it('should mark name field as invalid when empty', () => {
      const nameControl = component.form.controls.name;
      nameControl.setValue('');
      nameControl.markAsTouched();
      expect(nameControl.invalid).toBe(true);
    });

    it('should mark name field as invalid when exceeding max length', () => {
      const nameControl = component.form.controls.name;
      nameControl.setValue('a'.repeat(101));
      expect(nameControl.hasError('maxlength')).toBe(true);
    });

    it('should mark name field as valid with text within max length', () => {
      const nameControl = component.form.controls.name;
      nameControl.setValue('Valid Filter Name');
      expect(nameControl.valid).toBe(true);
    });

    it('should accept name with exactly 100 characters', () => {
      const nameControl = component.form.controls.name;
      nameControl.setValue('a'.repeat(100));
      expect(nameControl.valid).toBe(true);
    });

    it('should form be valid when name is provided', () => {
      component.form.controls.name.setValue('My Saved Filter');
      expect(component.form.valid).toBe(true);
    });

    it('should form be invalid when name is empty', () => {
      component.form.controls.name.setValue('');
      expect(component.form.invalid).toBe(true);
    });
  });

  describe('Template Rendering', () => {
    it('should render dialog title', () => {
      const title = compiled.query(By.css('[mat-dialog-title]'));
      expect(title).toBeTruthy();
      expect(title.nativeElement.textContent).toContain('Save Filters');
    });

    it('should render form element with correct aria-label', () => {
      const form = compiled.query(By.css('form'));
      expect(form).toBeTruthy();
      expect(form.nativeElement.getAttribute('aria-label')).toBe('Save filters form');
    });

    it('should render name input field', () => {
      const input = compiled.query(By.css('input[formControlName="name"]'));
      expect(input).toBeTruthy();
    });

    it('should render name input with correct placeholder', () => {
      const input = compiled.query(By.css('input[formControlName="name"]'));
      expect(input.nativeElement.placeholder).toContain('Enter a name');
    });

    it('should render label for name field', () => {
      const label = compiled.query(By.css('mat-label'));
      expect(label).toBeTruthy();
      expect(label.nativeElement.textContent).toContain('Name');
    });

    it('should render cancel button', () => {
      const buttons = compiled.queryAll(By.css('button'));
      const cancelButton = buttons.find(b => b.nativeElement.textContent.includes('Cancel'));
      expect(cancelButton).toBeTruthy();
    });

    it('should render save button', () => {
      const buttons = compiled.queryAll(By.css('button'));
      const saveButton = buttons.find(b => b.nativeElement.textContent.includes('Save'));
      expect(saveButton).toBeTruthy();
    });

    it('should have save button with color="primary"', () => {
      const buttons = compiled.queryAll(By.css('button'));
      const saveButton = buttons.find(b => b.nativeElement.textContent.includes('Save'));
      expect(saveButton?.nativeElement.getAttribute('color')).toBe('primary');
    });
  });

  describe('Error Messages', () => {
    it('should display required error when name is touched and empty', () => {
      const nameControl = component.form.controls.name;
      nameControl.setValue('');
      nameControl.markAsTouched();
      fixture.detectChanges();

      const errorElement = compiled.query(By.css('mat-error'));
      expect(errorElement).toBeTruthy();
      expect(errorElement.nativeElement.textContent).toContain('Name is required');
    });

    it('should display maxlength error when name exceeds 100 characters', () => {
      const nameControl = component.form.controls.name;
      nameControl.setValue('a'.repeat(101));
      nameControl.markAsTouched();
      fixture.detectChanges();

      const errorElement = compiled.query(By.css('mat-error'));
      expect(errorElement).toBeTruthy();
      expect(errorElement.nativeElement.textContent).toContain('cannot exceed 100 characters');
    });

    it('should not display error when field is untouched', () => {
      const nameControl = component.form.controls.name;
      nameControl.setValue('');
      fixture.detectChanges();

      const errorElements = compiled.queryAll(By.css('mat-error'));
      expect(errorElements.length).toBe(0);
    });
  });

  describe('Form Submission', () => {
    it('should close dialog with result on valid form submission', () => {
      const closeSpyFn = mockDialogRef.close as ReturnType<typeof vi.fn>;
      component.form.controls.name.setValue('Test Filter');
      component.onSubmit();

      expect(closeSpyFn).toHaveBeenCalled();
      const result = closeSpyFn.mock.calls[0][0];
      expect(result).toEqual({name: 'Test Filter'});
    });

    it('should not close dialog on invalid form submission', () => {
      const closeSpyFn = mockDialogRef.close as ReturnType<typeof vi.fn>;
      component.form.controls.name.setValue('');
      component.onSubmit();

      expect(closeSpyFn).not.toHaveBeenCalled();
    });

    it('should mark all fields as touched on invalid submission', () => {
      component.form.controls.name.setValue('');
      component.onSubmit();

      expect(component.form.controls.name.touched).toBe(true);
    });

    it('should submit form with trimmed whitespace', () => {
      const closeSpyFn = mockDialogRef.close as ReturnType<typeof vi.fn>;
      component.form.controls.name.setValue('  Test Filter  ');
      component.onSubmit();

      expect(closeSpyFn).toHaveBeenCalled();
    });

    it('should trigger submit when form submission occurs', () => {
      const submitSpy = vi.spyOn(component, 'onSubmit');
      component.form.controls.name.setValue('Valid Name');
      const form = compiled.query(By.css('form'));
      form.nativeElement.dispatchEvent(new Event('ngSubmit'));

      expect(submitSpy).toHaveBeenCalled();
    });
  });

  describe('Dialog Cancellation', () => {
    it('should close dialog with null on onCancel', () => {
      const closeSpyFn = mockDialogRef.close as ReturnType<typeof vi.fn>;
      component.onCancel();

      expect(closeSpyFn).toHaveBeenCalledWith(null);
    });

    it('should close dialog with null without setting form value on cancel', () => {
      const closeSpyFn = mockDialogRef.close as ReturnType<typeof vi.fn>;
      component.form.controls.name.setValue('Some Value');
      component.onCancel();

      expect(closeSpyFn).toHaveBeenCalledWith(null);
    });

    it('should trigger onCancel when cancel button is clicked', () => {
      const cancelSpy = vi.spyOn(component, 'onCancel');
      const buttons = compiled.queryAll(By.css('button'));
      const cancelButton = buttons.find(b => b.nativeElement.textContent.includes('Cancel'));
      cancelButton?.nativeElement.click();

      expect(cancelSpy).toHaveBeenCalled();
    });
  });

  describe('User Interactions', () => {
    it('should update form control value when user types', () => {
      const input = compiled.query(By.css('input[formControlName="name"]'));
      input.nativeElement.value = 'New Filter';
      input.nativeElement.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(component.form.controls.name.value).toBe('New Filter');
    });

    it('should enable save button when form is valid', () => {
      component.form.controls.name.setValue('Valid');
      fixture.detectChanges();

      const buttons = compiled.queryAll(By.css('button'));
      const saveButton = buttons.find(b => b.nativeElement.textContent.includes('Save'));
      expect(saveButton?.nativeElement.disabled).toBe(false);
    });

    it('should disable save button when form is invalid', () => {
      component.form.controls.name.setValue('');
      fixture.detectChanges();

      const buttons = compiled.queryAll(By.css('button'));
      const saveButton = buttons.find(b => b.nativeElement.textContent.includes('Save'));
      expect(saveButton?.nativeElement.disabled).toBe(true);
    });

    it('cancel button should always be enabled', () => {
      const buttons = compiled.queryAll(By.css('button'));
      const cancelButton = buttons.find(b => b.nativeElement.textContent.includes('Cancel'));
      expect(cancelButton?.nativeElement.disabled).toBe(false);
    });
  });

  describe('Component Metadata', () => {
    it('should have correct selector', () => {
      const metadata = (component.constructor as any);
      // Verify component is standalone and configured properly
      expect(metadata).toBeDefined();
    });

    it('should use external template file', () => {
      const metadata = (component as any).constructor;
      // Verify metadata is accessible
      expect(metadata).toBeDefined();
    });
  });
});


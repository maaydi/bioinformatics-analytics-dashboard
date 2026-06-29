import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {MatDialogRef} from '@angular/material/dialog';
import {of} from 'rxjs';
import {AuthService} from '@core/services/auth.service';
import {AccountSettingsComponent} from './account-settings.component';
import {beforeEach, describe, expect, it, vi} from 'vitest';

describe('AccountSettingsComponent', () => {
  let component: AccountSettingsComponent;
  let fixture: ComponentFixture<AccountSettingsComponent>;
  let mockDialogRef: Partial<MatDialogRef<AccountSettingsComponent>>;
  let mockAuthService: Partial<AuthService>;

  beforeEach(async () => {
    mockDialogRef = {
      close: vi.fn(),
    };
    mockAuthService = {
      changePassword: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AccountSettingsComponent, ReactiveFormsModule],
      providers: [
        {provide: MatDialogRef, useValue: mockDialogRef},
        {provide: AuthService, useValue: mockAuthService},
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Form validation', () => {
    it('should initialize with an empty, invalid form', () => {
      expect(component.form.valid).toBe(false);
      expect(component.form.value).toEqual({
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
      });
    });

    it('should invalidate when new password does not meet complexity requirements', () => {
      const newPasswordControl = component.form.controls.newPassword;
      newPasswordControl.setValue('weak');
      expect(newPasswordControl.errors?.['pattern']).toBeTruthy();
    });

    it('should validate when new password meets complexity requirements', () => {
      const newPasswordControl = component.form.controls.newPassword;
      newPasswordControl.setValue('StrongPassword123!');
      expect(newPasswordControl.valid).toBe(true);
    });

    it('should have passwordMismatch error when confirmPassword does not match newPassword', () => {
      component.form.patchValue({
        currentPassword: 'OldPassword123!',
        newPassword: 'StrongPassword123!',
        confirmPassword: 'DifferentPassword123!',
      });
      component.form.updateValueAndValidity();

      expect(component.form.errors?.['passwordMismatch']).toBe(true);
      expect(component.form.valid).toBe(false);
    });

    it('should have a valid form when confirmPassword matches newPassword', () => {
      component.form.patchValue({
        currentPassword: 'OldPassword123!',
        newPassword: 'StrongPassword123!',
        confirmPassword: 'StrongPassword123!',
      });
      component.form.updateValueAndValidity();
      expect(component.form.errors).toBeNull();
      expect(component.form.valid).toBe(true);
    });
  });

  describe('onSubmit', () => {
    it('should mark all controls as touched if form is invalid', () => {
      const markAllAsTouchedSpy = vi.spyOn(component.form, 'markAllAsTouched');
      component.onSubmit();
      expect(markAllAsTouchedSpy).toHaveBeenCalled();
      expect(mockAuthService.changePassword).not.toHaveBeenCalled();
    });

    it('should call authService and close dialog on success', () => {
      component.form.patchValue({
        currentPassword: 'OldPassword123!',
        newPassword: 'StrongPassword123!',
        confirmPassword: 'StrongPassword123!',
      });
      (mockAuthService.changePassword as ReturnType<typeof vi.fn>).mockReturnValue(
        of({success: true}),
      );

      component.onSubmit();

      expect(mockAuthService.changePassword).toHaveBeenCalledWith({
        currentPassword: 'OldPassword123!',
        newPassword: 'StrongPassword123!',
      });
      expect(mockDialogRef.close).toHaveBeenCalledWith({success: true});
    });

    it('should set error message on failure', () => {
      component.form.patchValue({
        currentPassword: 'OldPassword123!',
        newPassword: 'StrongPassword123!',
        confirmPassword: 'StrongPassword123!',
      });
      const errorMessage = 'Invalid current password';
      (mockAuthService.changePassword as ReturnType<typeof vi.fn>).mockReturnValue(
        of({success: false, message: errorMessage}),
      );

      component.onSubmit();

      expect((component as any).error()).toBe(errorMessage);
      expect(mockDialogRef.close).not.toHaveBeenCalled();
    });

    it('should set default error message on failure when no message provided', () => {
      component.form.patchValue({
        currentPassword: 'OldPassword123!',
        newPassword: 'StrongPassword123!',
        confirmPassword: 'StrongPassword123!',
      });
      (mockAuthService.changePassword as ReturnType<typeof vi.fn>).mockReturnValue(
        of({success: false}),
      );

      component.onSubmit();

      expect((component as any).error()).toBe('Password change failed');
      expect(mockDialogRef.close).not.toHaveBeenCalled();
    });
  });

  describe('onCancel', () => {
    it('should close dialog with success: false', () => {
      component.onCancel();
      expect(mockDialogRef.close).toHaveBeenCalledWith({success: false});
    });
  });
});

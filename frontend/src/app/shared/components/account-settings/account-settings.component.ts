import {Component, inject, signal} from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import {MatDialogModule, MatDialogRef} from '@angular/material/dialog';
import {MatError, MatFormField, MatLabel} from '@angular/material/form-field';
import {MatInput} from '@angular/material/input';
import {MatButton} from '@angular/material/button';
import {AuthService} from '@core/services/auth.service';

export const passwordMatchValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const newPassword = control.get('newPassword');
  const confirmPassword = control.get('confirmPassword');

  return newPassword && confirmPassword && newPassword.value !== confirmPassword.value
    ? {passwordMismatch: true}
    : null;
};

@Component({
  selector: 'app-account-settings',
  imports: [
    ReactiveFormsModule,
    MatFormField,
    MatLabel,
    MatInput,
    MatError,
    MatButton,
    MatDialogModule,
  ],
  templateUrl: './account-settings.component.html',
  styleUrl: './account-settings.component.scss',
})
export class AccountSettingsComponent {
  readonly form = new FormGroup(
    {
      currentPassword: new FormControl<string>('', {
        nonNullable: true,
        validators: [Validators.required],
      }),
      newPassword: new FormControl<string>('', {
        nonNullable: true,
        validators: [
          Validators.required,
          Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{12,}$/),
        ],
      }),
      confirmPassword: new FormControl<string>('', {
        nonNullable: true,
        validators: [Validators.required],
      }),
    },
    {validators: passwordMatchValidator},
  );

  protected error = signal<string | null>(null);

  private readonly dialogRef = inject(MatDialogRef<AccountSettingsComponent>);
  private readonly authService: AuthService = inject(AuthService);

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.error.set(null);
    this.authService
      .changePassword({
        currentPassword: this.form.controls.currentPassword.value,
        newPassword: this.form.controls.newPassword.value,
      })
      .subscribe({
        next: (result) => {
          if (result.success) {
            this.dialogRef.close({success: true});
          } else {
            this.error.set(`${result.message || 'Password change failed'}`);
          }
        },
      });
  }

  onCancel(): void {
    this.dialogRef.close({success: false});
  }
}

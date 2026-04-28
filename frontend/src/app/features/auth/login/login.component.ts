import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../../core/services/auth.service';

/**
 * Login page — public route (/login).
 *
 * Validation (documentation/validation-rules.md §4):
 * - username: required, min 3, max 50
 * - password: required, min 8
 *
 * On success: redirects to /
 * On 401: shows "Invalid credentials" error message
 *
 * TODO: implement full reactive form + error handling in ticket AUTH-001
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule,
  ],
  template: `
    <div class="login-wrapper">
      <mat-card class="login-card">
        <mat-card-header>
          <mat-card-title>Bioinformatics Dashboard</mat-card-title>
          <mat-card-subtitle>Sign in to continue</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Username</mat-label>
              <input matInput formControlName="username" autocomplete="username" />
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Password</mat-label>
              <input matInput type="password" formControlName="password" autocomplete="current-password" />
            </mat-form-field>
            @if (errorMessage) {
              <p class="error-text">{{ errorMessage }}</p>
            }
            <button mat-raised-button color="primary" type="submit" class="full-width"
                    [disabled]="form.invalid || loading">
              {{ loading ? 'Signing in…' : 'Sign In' }}
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .login-wrapper { display: flex; justify-content: center; align-items: center; min-height: 100vh; }
    .login-card { width: 380px; padding: 16px; }
    .full-width { width: 100%; margin-bottom: 12px; display: block; }
    .error-text { color: red; font-size: 14px; margin-bottom: 8px; }
  `],
})
export class LoginComponent {

  private readonly auth   = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb     = inject(FormBuilder);

  form: FormGroup = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  loading      = false;
  errorMessage = '';

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading      = true;
    this.errorMessage = '';

    this.auth.login(this.form.value).subscribe({
      next:  () => this.router.navigate(['/']),
      error: () => {
        this.loading      = false;
        this.errorMessage = 'Invalid credentials. Please try again.';
      },
    });
  }
}

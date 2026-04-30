import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
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
 */
@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly form: FormGroup = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  readonly loading = signal(false);
  readonly errorMessage = signal('');

  onSubmit(): void {
    if (this.form.invalid) {
      return;
    }
    this.loading.set(true);
    this.errorMessage.set('');

    this.auth.login(this.form.value).subscribe({
      next: () => this.router.navigate(['/']),
      error: () => {
        this.loading.set(false);
        this.errorMessage.set('Invalid credentials. Please try again.');
      },
    });
  }
}

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

/**
 * Reusable loading spinner.
 * Used in all feature components while awaiting API responses.
 * Satisfies the "loading state" requirement from documentation/constitution.md.
 */
@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule],
  template: `
    <div class="spinner-wrapper" role="status" aria-label="Loading">
      <mat-spinner diameter="48" />
    </div>
  `,
  styles: [`
    .spinner-wrapper {
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 64px 0;
    }
  `],
})
export class LoadingSpinnerComponent {}

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

/**
 * Reusable loading spinner.
 * Used in all feature components while awaiting API responses.
 * Satisfies the "loading state" requirement from documentation/constitution.md.
 */
@Component({
  selector: 'app-loading-spinner',
  imports: [MatProgressSpinnerModule],
  templateUrl: './loading-spinner.component.html',
  styleUrl: './loading-spinner.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoadingSpinnerComponent {}

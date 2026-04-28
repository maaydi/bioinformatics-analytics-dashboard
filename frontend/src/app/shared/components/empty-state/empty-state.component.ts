import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/**
 * Reusable empty-state placeholder.
 * Shown when a list/table has zero results.
 * Required by constitution.md: "Loading / error / empty states required."
 */
@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <div class="empty-state">
      <mat-icon class="empty-icon">{{ icon }}</mat-icon>
      <p class="empty-message">{{ message }}</p>
    </div>
  `,
  styles: [`
    .empty-state {
      display: flex; flex-direction: column;
      align-items: center; justify-content: center;
      padding: 64px 0; color: #9e9e9e;
    }
    .empty-icon { font-size: 48px; height: 48px; width: 48px; margin-bottom: 16px; }
    .empty-message { font-size: 16px; }
  `],
})
export class EmptyStateComponent {
  @Input() message = 'No data found';
  @Input() icon    = 'search_off';
}

import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/**
 * Reusable empty-state placeholder.
 * Shown when a list/table has zero results.
 * Required by constitution.md: "Loading / error / empty states required."
 */
@Component({
  selector: 'app-empty-state',
  imports: [MatIconModule],
  templateUrl: './empty-state.component.html',
  styleUrl: './empty-state.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyStateComponent {
  readonly message = input('No data found');
  readonly icon = input('search_off');
}

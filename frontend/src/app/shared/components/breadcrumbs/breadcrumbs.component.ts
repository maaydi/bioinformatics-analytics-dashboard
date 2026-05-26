import {ChangeDetectionStrategy, Component, input} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';
import {MatIconModule} from '@angular/material/icon';

/**
 * Breadcrumb navigation component compliant with application design system.
 * Displays navigation hierarchy and supports active/inactive states.
 * Respects design tokens (typography, spacing, colors) from _design-system.scss.
 *
 * Usage:
 * ```html
 * <app-breadcrumbs
 *   [items]="breadcrumbItems()"
 * />
 * ```
 *
 * Where items is a Signal<readonly BreadcrumbItem[]>:
 * ```TypeScript
 * readonly breadcrumbItems = signal<readonly BreadcrumbItem[]>([
 *   { label: 'Home', routerLink: ['/'] },
 *   { label: 'Genes', routerLink: ['/genes'] },
 *   { label: 'Pro50024', isActive: true }
 * ]);
 * ```
 */

export interface BreadcrumbItem {
  readonly label: string;
  readonly routerLink?: string[];
  readonly isActive?: boolean;
}

@Component({
  selector: 'app-breadcrumbs',
  imports: [CommonModule, RouterModule, MatIconModule],
  templateUrl: './breadcrumbs.component.html',
  styleUrl: './breadcrumbs.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BreadcrumbsComponent {
  /**
   * Breadcrumb items to display.
   * Signal-based input for reactive updates.
   */
  readonly items = input<readonly BreadcrumbItem[]>([]);

  /**
   * Track function for @for loop optimization.
   * Prevents unnecessary re-renders when list items change.
   */
  trackByLabel(_index: number, item: BreadcrumbItem): string {
    return item.label;
  }
}

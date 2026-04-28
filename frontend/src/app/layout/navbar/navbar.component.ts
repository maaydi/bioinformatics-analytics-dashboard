import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/services/auth.service';

/**
 * Top navigation bar.
 * Shows links for all main sections and a logout button.
 * Admin-only link (/admin/import) is conditionally shown based on role.
 */
@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, MatToolbarModule, MatButtonModule, MatIconModule],
  template: `
    <mat-toolbar color="primary">
      <span class="app-title">BioInfo Dashboard</span>
      <span class="spacer"></span>

      <a mat-button routerLink="/"             routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}">Dashboard</a>
      <a mat-button routerLink="/genes"        routerLinkActive="active">Gene Explorer</a>
      <a mat-button routerLink="/analytics"    routerLinkActive="active">Analytics</a>
      <a mat-button routerLink="/saved-filters" routerLinkActive="active">Saved Filters</a>

      @if (authService.isAdmin()) {
        <a mat-button routerLink="/admin/import" routerLinkActive="active">Import</a>
      }

      <button mat-icon-button (click)="authService.logout()" title="Logout">
        <mat-icon>logout</mat-icon>
      </button>
    </mat-toolbar>
  `,
  styles: [`
    .spacer { flex: 1 1 auto; }
    .app-title { font-weight: 600; margin-right: 16px; }
    a.active { background: rgba(255,255,255,0.15); border-radius: 4px; }
  `],
})
export class NavbarComponent {
  readonly authService = inject(AuthService);
}

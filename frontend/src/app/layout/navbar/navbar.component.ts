import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatToolbarModule} from '@angular/material/toolbar';
import {Router, RouterLink, RouterLinkActive} from '@angular/router';
import {AuthService} from '@core/services/auth.service';
import {ThemeService} from '@core/services/theme.service';
import {MatDialog} from '@angular/material/dialog';
import {AccountSettingsComponent} from '@shared/components/account-settings/account-settings.component';

/**
 * Top navigation bar.
 * Shows links for all main sections and a logout button.
 * Admin-only link (/admin/import) is conditionally shown based on role.
 */
@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive, MatToolbarModule, MatButtonModule, MatIconModule],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavbarComponent {
  readonly authService = inject(AuthService);
  protected readonly themeService = inject(ThemeService);
  private readonly router = inject(Router);

  constructor(private dialog: MatDialog) {
  }

  protected openAccountSettingsDialog() {
    const dialogRef = this.dialog.open(AccountSettingsComponent, {
      width: '600px',
    });

    dialogRef.afterClosed().subscribe((response: { success: boolean } | null) => {
      if (response?.success) {
        this.router.navigate(['/login']).then(() => {
          console.log('Navigated to /login after password change');
        });
      }
    });
  }
}

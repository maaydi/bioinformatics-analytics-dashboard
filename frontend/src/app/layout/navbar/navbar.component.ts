import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatToolbarModule} from '@angular/material/toolbar';
import {RouterLink, RouterLinkActive} from '@angular/router';
import {AuthService} from '@core/services/auth.service';

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
}

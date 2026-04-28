import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../navbar/navbar.component';
import { MatSidenavModule } from '@angular/material/sidenav';

/**
 * Shell layout wrapping all authenticated routes.
 * Contains the top navbar and a router outlet for feature pages.
 */
@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, MatSidenavModule],
  template: `
    <div class="layout-wrapper">
      <app-navbar />
      <main class="layout-content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [`
    .layout-wrapper { display: flex; flex-direction: column; height: 100%; }
    .layout-content { flex: 1; padding: 24px; overflow: auto; }
  `],
})
export class MainLayoutComponent {}

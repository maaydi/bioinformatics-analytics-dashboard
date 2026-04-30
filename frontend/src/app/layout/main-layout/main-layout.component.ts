import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatSidenavModule } from '@angular/material/sidenav';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../navbar/navbar.component';

/**
 * Shell layout wrapping all authenticated routes.
 * Contains the top navbar and a router outlet for feature pages.
 */
@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, NavbarComponent, MatSidenavModule],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayoutComponent {}

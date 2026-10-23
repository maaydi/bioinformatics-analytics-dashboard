import {ChangeDetectionStrategy, Component, DestroyRef, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {MatSidenavModule} from '@angular/material/sidenav';
import {Router, RouterOutlet} from '@angular/router';
import {NotificationService} from '@shared/directive/notification.service';
import {ExportPipelineCompletionMonitor} from '@features/export/export-pipeline-completion-monitor.service';
import {NavbarComponent} from '../navbar/navbar.component';

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
export class MainLayoutComponent {
  private readonly router = inject(Router);
  private readonly notify = inject(NotificationService);
  private readonly monitor = inject(ExportPipelineCompletionMonitor);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.monitor.completedPipeline$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(pipeline => {
      this.notify.show(`Export Pipeline "${pipeline.name}" is ready.`, 'View', {duration: 5000})
        .onAction()
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(() => void this.router.navigate(['/exports']));
    });
  }
}

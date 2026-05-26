import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  input,
  OnInit,
  PLATFORM_ID,
  signal
} from '@angular/core';
import {DOCUMENT, isPlatformBrowser} from '@angular/common';
import {MatTabsModule} from '@angular/material/tabs';
import {GenesService} from '@features/genes/genes.service';
import {BreadcrumbItem, BreadcrumbsComponent} from '@shared/components/breadcrumbs/breadcrumbs.component';
import {EVIDENCE_LEVEL_LABELS, ProteinDetail} from '@core/models/protein.model';
import {LoadingSpinnerComponent} from '@shared/components/loading-spinner/loading-spinner.component';
import {MatCard} from '@angular/material/card';
import {MatError} from '@angular/material/input';
import {MatIcon} from '@angular/material/icon';
import {MatTooltip} from '@angular/material/tooltip';
import {MatButtonModule} from '@angular/material/button';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';

/**
 * Gene Detail page — Epic 5 (US-15, US-16, US-17).
 *
 * Displays a single protein entry with tabs:
 *   Summary | Sequence | Features | Cross References | Taxonomy | Publications | Similar Proteins
 *
 * Route: /genes/:id  (id bound via input() with withComponentInputBinding)
 * Data source: GET /api/genes/{id} → ProteinDetail
 * NFR: ≤ 1 s load time (§12.1)
 *
 * TODO: implement in ticket DETAIL-001
 */
@Component({
  selector: 'app-gene-detail',
  imports: [
    MatTabsModule,
    BreadcrumbsComponent,
    LoadingSpinnerComponent,
    MatCard,
    MatError,
    MatIcon,
    MatTooltip,
    MatButtonModule
  ],
  templateUrl: './gene-detail.component.html',
  styleUrl: './gene-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GeneDetailComponent implements OnInit {
  loading = signal<boolean>(true);
  readonly id = input.required<number>();
  proteinDetails = signal<ProteinDetail | null>(null);
  errorMessage = signal<string | null>(null);
  readonly copyFeedbackMessage = signal<string | null>(null);
  readonly breadcrumbItems = computed<readonly BreadcrumbItem[]>(() => {
    return [
      {label: 'Dashboard', routerLink: ['/']},
      {label: 'Gene Explorer', routerLink: ['/genes']},
      {label: 'Gene Detail', isActive: true},
    ];
  });
  private readonly service = inject(GenesService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly document = inject(DOCUMENT);
  private readonly platformId = inject(PLATFORM_ID);
  private clearCopyMessageTimerId: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.service.getGeneById(this.id())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          this.proteinDetails.set(result);
          this.errorMessage.set(null);
          this.loading.set(false);
        },
        error: () => {
          this.errorMessage.set('Failed to load protein details.');
          this.loading.set(false);
        }
      });
  }

  evidenceLevelStr(): string {
    const level = this.proteinDetails()?.evidenceLevel;
    if (level) {
      return `${EVIDENCE_LEVEL_LABELS[level]} (Evidence level ${level})`;
    }
    return 'Unknown Evidence level';
  }

  reviewBadgeClass(): string {
    return `review-badge ${this.proteinDetails()?.reviewed ? 'is-reviewed' : 'not-reviewed'}`;
  }

  evidenceBadgeClass(): string {
    return `evidence-badge level-${this.proteinDetails()?.evidenceLevel ?? 'undefined'}`;
  }

  async copyAccessionToClipboard(): Promise<void> {
    const accession = this.proteinDetails()?.accession;
    if (!accession || !isPlatformBrowser(this.platformId)) {
      return;
    }

    try {
      const clipboard = globalThis.navigator?.clipboard;
      if (clipboard) {
        await clipboard.writeText(accession);
      } else {
        this.copyUsingSelectionFallback(accession);
      }

      this.showCopyFeedback('Accession copied.');
    } catch {
      this.showCopyFeedback('Unable to copy accession.');
    }
  }

  copyTooltipLabel(): string {
    return this.copyFeedbackMessage() ?? 'Copy accession';
  }

  private copyUsingSelectionFallback(value: string): void {
    const tempTextArea = this.document.createElement('textarea');
    tempTextArea.value = value;
    tempTextArea.style.position = 'fixed';
    tempTextArea.style.opacity = '0';
    this.document.body.append(tempTextArea);
    tempTextArea.focus();
    tempTextArea.select();
    this.document.execCommand('copy');
    tempTextArea.remove();
  }

  private showCopyFeedback(message: string): void {
    if (this.clearCopyMessageTimerId) {
      clearTimeout(this.clearCopyMessageTimerId);
    }

    this.copyFeedbackMessage.set(message);
    this.clearCopyMessageTimerId = setTimeout(() => {
      this.copyFeedbackMessage.set(null);
    }, 1800);
  }
}

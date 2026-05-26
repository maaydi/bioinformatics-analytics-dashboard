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
import {isPlatformBrowser} from '@angular/common';
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
      await navigator.clipboard.writeText(accession);
      this.showCopyFeedback('Accession copied.');
    } catch {
      this.showCopyFeedback('Unable to copy accession.');
    }
  }

  copyTooltipLabel(): string {
    return this.copyFeedbackMessage() ?? 'Copy accession';
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

  /** Format a date string (ISO 8601) to human-readable format. */
  formatDate(dateStr: string | null | undefined): string {
    return dateStr ? new Date(dateStr).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    }) : '–';
  }

  /** Returns a CSS modifier class for a given featureType chip. */
  featureTypeClass(featureType: string): string {
    return `feature-type-chip feature-type-chip--${featureType.toLowerCase()}`;
  }

  /** Strip surrounding quotes from a raw API note/featureId/evidence string. */
  stripQuotes(value: string | null | undefined): string {
    if (!value) return '–';
    return value.replace(/^"|"$/g, '');
  }

  /** Build external URL for known cross-reference providers. */
  crossReferenceUrl(source: string | null | undefined, identifier: string | null | undefined): string | null {
    if (!source || !identifier || identifier === '-') {
      return null;
    }

    const encodedIdentifier = encodeURIComponent(identifier);
    switch (source) {
      case 'RefSeq':
        return `https://www.ncbi.nlm.nih.gov/protein/${encodedIdentifier}`;
      case 'KEGG':
        return `https://www.genome.jp/entry/${encodedIdentifier}`;
      case 'EMBL':
        return `https://www.ebi.ac.uk/ena/browser/view/${encodedIdentifier}`;
      case 'Proteomes':
        return `https://www.uniprot.org/proteomes/${encodedIdentifier}`;
      case 'OrthoDB':
        return `https://www.orthodb.org/?query=${encodedIdentifier}`;
      default:
        return null;
    }
  }

  /** Safely parse molecular weight as number with K suffix if > 1000. */
  formatMolecularWeight(): string {
    const weight = this.proteinDetails()?.molecularWeight;
    if (!weight) return '–';
    return weight >= 1000 ? `${(weight / 1000).toFixed(1)}K Da` : `${weight} Da`;
  }

  /** Get display text for null/empty arrays. */
  getDisplayValue(value: string | string[] | null | undefined): string {
    if (!value) return '–';
    if (Array.isArray(value)) return value.length > 0 ? value.join(', ') : '–';
    return value;
  }
}

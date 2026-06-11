import {ChangeDetectionStrategy, Component, computed, ElementRef, inject, ViewChild} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {DecimalPipe} from '@angular/common';
import {LoadingSpinnerComponent} from '@shared/components/loading-spinner/loading-spinner.component';
import {AbstractDashboardEvidenceLevelsDirective} from '@shared/directive/dashboard-evidence-level.directive';
import {MatIcon} from '@angular/material/icon';
import {ImageExportService} from '@shared/directive/image-export-service';

interface EvidenceLevelView {
  readonly level: number;
  readonly label: string;
  readonly count: number;
  readonly ratioPercent: number;
  readonly colorClass: string;
}

@Component({
  selector: 'app-dashboard-evidence-levels',
  imports: [MatCardModule, DecimalPipe, LoadingSpinnerComponent, MatButtonModule, MatIcon],
  templateUrl: './dashboard-evidence-levels.component.html',
  styleUrl: './dashboard-evidence-levels.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardEvidenceLevelsComponent extends AbstractDashboardEvidenceLevelsDirective {
  @ViewChild('chartCard', {read: ElementRef})
  chartCard!: ElementRef<HTMLElement>;

  private readonly imageExportService = inject(ImageExportService);

  protected readonly items = computed<ReadonlyArray<EvidenceLevelView>>(() => {
    const items = this.sortedItems();
    const maxCount = Math.max(...items.map((item) => item.count), 0);

    return items.map((item) => ({
      level: item.evidenceLevel,
      label: item.label,
      count: item.count,
      ratioPercent: maxCount > 0 ? Math.round((item.count / maxCount) * 100) : 0,
      colorClass: this.toColorClass(item.evidenceLevel),
    }));
  });

  protected async exportAsImage(): Promise<void> {
    if (!this.chartCard) return;

    await this.imageExportService.exportElement(
      this.chartCard.nativeElement,
      'evidence-levels-dashboard',
      '.no-export'
    );
  }
}

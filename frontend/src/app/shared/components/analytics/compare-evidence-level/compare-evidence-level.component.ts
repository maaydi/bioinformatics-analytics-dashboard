import {ChangeDetectionStrategy, Component, computed, input} from '@angular/core';
import {MatCardModule} from '@angular/material/card';
import {DecimalPipe} from '@angular/common';
import {EvidenceLevelItem} from '@core/models/analytics.model';

interface CompareRowView {
  readonly level: number;
  readonly label: string;
  readonly primaryCount: number;
  readonly secondaryCount: number;
  readonly primaryRatio: number;
  readonly secondaryRatio: number;
  readonly colorClass: string;
}

@Component({
  selector: 'app-compare-evidence-level',
  imports: [MatCardModule, DecimalPipe],
  templateUrl: './compare-evidence-level.component.html',
  styleUrl: './compare-evidence-level.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CompareEvidenceLevelComponent {
  public readonly primaryData = input<ReadonlyArray<EvidenceLevelItem>>([]);
  public readonly secondaryData = input<ReadonlyArray<EvidenceLevelItem>>([]);

  public readonly primaryLabel = input<string>('Base Filter');
  public readonly secondaryLabel = input<string>('Compare Filter');

  protected readonly items = computed<ReadonlyArray<CompareRowView>>(() => {
    const primary = this.primaryData();
    const secondary = this.secondaryData();

    const levelsMap = new Map<number, { label: string; primary: number; secondary: number }>();

    const processData = (data: ReadonlyArray<EvidenceLevelItem>, isPrimary: boolean) => {
      for (const item of data) {
        if (!levelsMap.has(item.evidenceLevel)) {
          levelsMap.set(item.evidenceLevel, {label: item.label, primary: 0, secondary: 0});
        }
        const entry = levelsMap.get(item.evidenceLevel)!;
        if (isPrimary) {
          entry.primary = item.count;
        } else {
          entry.secondary = item.count;
        }
      }
    };

    processData(primary, true);
    processData(secondary, false);

    const allCounts = Array.from(levelsMap.values()).flatMap(v => [v.primary, v.secondary]);
    const maxCount = Math.max(...allCounts, 0);

    return Array.from(levelsMap.entries())
      .sort(([levelA], [levelB]) => levelA - levelB)
      .map(([level, data]) => ({
        level,
        label: data.label,
        primaryCount: data.primary,
        secondaryCount: data.secondary,
        primaryRatio: maxCount > 0 ? Math.round((data.primary / maxCount) * 100) : 0,
        secondaryRatio: maxCount > 0 ? Math.round((data.secondary / maxCount) * 100) : 0,
        colorClass: this.toColorClass(level),
      }));
  });

  protected readonly hasData = computed<boolean>(() => this.items().length > 0);

  protected toColorClass(evidenceLevel: number): string {
    switch (evidenceLevel) {
      case 1:
        return 'level-l1';
      case 2:
        return 'level-l2';
      case 3:
        return 'level-l3';
      case 4:
        return 'level-l4';
      case 5:
        return 'level-l5';
      default:
        return 'level-other';
    }
  }
}

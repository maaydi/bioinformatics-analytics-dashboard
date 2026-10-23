import {ChangeDetectionStrategy, Component, input, output} from '@angular/core';
import {MatIcon} from '@angular/material/icon';
import {FilterChip} from '@shared/utils/filter-chips-builder';
import {CHIP_LIST_MESSAGES} from '@shared/messages/ui.messages';


@Component({
  selector: 'app-chip-list',
  imports: [
    MatIcon],
  templateUrl: './chip-list.component.html',
  styleUrl: './chip-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChipListComponent {
  protected readonly messages = CHIP_LIST_MESSAGES;
  readonly chips = input<FilterChip[]>([]);
  readonly readOnly = input<boolean>(true);
  readonly ariaLabel = input<string>(CHIP_LIST_MESSAGES.label);
  readonly chipRemoved = output<FilterChip['key']>();

  protected removeChip(key: FilterChip['key']): void {
    this.chipRemoved.emit(key);
  }
}

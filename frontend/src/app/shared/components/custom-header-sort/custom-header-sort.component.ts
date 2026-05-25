import {Component, computed, input, output} from '@angular/core';
import {MatIcon} from '@angular/material/icon';


export type SortDirection = 'asc' | 'desc' | 'none';

export interface SortExchangeEvent {
  field: string;
  direction: SortDirection;
}

@Component({
  selector: 'app-custom-header-sort',
  imports: [
    MatIcon
  ],
  templateUrl: './custom-header-sort.component.html',
  styleUrl: './custom-header-sort.component.scss',
})
export class CustomHeaderSortComponent {
  label = input.required<string>();
  field = input.required<string>();
  activeSortField = input<string>('id');
  activeSortDirection = input<SortDirection>('asc');

  sortChange = output<SortExchangeEvent>();
  isCurrentField = computed(() => this.activeSortField() === this.field());

  onHeaderClick(): void {
    let next: SortDirection = 'asc';
    if (this.isCurrentField()) {
      if (this.activeSortDirection() === 'asc') {
        next = 'desc';
      } else if (this.activeSortDirection() === 'desc') {
        next = 'none';
      }
    }
    if (next === 'none') {
      this.sortChange.emit({field: 'id', direction: 'asc'});
    } else {
      this.sortChange.emit({field: this.field(), direction: next});
    }
  }
}

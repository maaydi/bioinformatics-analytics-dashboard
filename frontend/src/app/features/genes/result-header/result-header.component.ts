import {ChangeDetectionStrategy, Component, computed, input, output} from '@angular/core';
import {MatPaginator, PageEvent} from '@angular/material/paginator';
import {PagedResponse} from '@core/models/paged-response.model';
import {ProteinSummary} from '@core/models/protein.model';
import {GeneFilterPageSort} from '@core/models/saved-filter.model';
import {RESULT_HEADER_MESSAGES} from '@features/genes/genes.messages';

@Component({
  selector: 'app-result-header',
  imports: [
    MatPaginator
  ],
  templateUrl: './result-header.component.html',
  styleUrl: './result-header.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResultHeaderComponent {
  protected readonly messages = RESULT_HEADER_MESSAGES;
  readonly data = input<PagedResponse<ProteinSummary> | null>(null);
  readonly updatePage = output<GeneFilterPageSort>();

  readonly totalGenes = computed<number>(() => this.data()?.totalElements ?? 0);
  readonly pageSize = computed<number>(() => this.data()?.size ?? 50);
  readonly pageIndex = computed<number>(() => this.data()?.page ?? 0);
  readonly totalPages = computed<number>(() => this.data()?.totalPages ?? 0);

  onPageChange(event: PageEvent) {
    let pageNumber = event.pageIndex;
    if (event.pageSize !== this.pageSize()) {
      pageNumber = 0;
    }
    this.updatePage.emit({page: pageNumber, size: event.pageSize});
  }
}
